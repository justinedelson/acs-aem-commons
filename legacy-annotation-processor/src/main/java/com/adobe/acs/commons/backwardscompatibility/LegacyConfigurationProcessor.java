/*
 * #%L
 * ACS AEM Commons Legacy Config Processor
 * %%
 * Copyright (C) 2018 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.backwardscompatibility;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("com.adobe.acs.commons.backwardscompatibility.LegacyConfiguration")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class LegacyConfigurationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<String, Definition> definitions = new HashMap<>();

        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : annotatedElements) {
                ExecutableElement method = (ExecutableElement) element;
                TypeElement container = (TypeElement) element.getEnclosingElement();
                String className = container.getQualifiedName().toString();
                if (!definitions.containsKey(className)) {
                    definitions.put(className, new Definition(container));
                }
                definitions.get(className).enableLegacy(method, method.getAnnotation(LegacyConfiguration.class).value());
            }

        }

        definitions.entrySet().forEach(e -> {
            final String name = e.getKey();
            int lastDotIndex = name.lastIndexOf('.');
            String newName = name.substring(0, lastDotIndex) + "Legacy" + name.substring(lastDotIndex + 1);

            int secondToLastDotIndex = name.lastIndexOf('.', lastDotIndex - 1);
            String localAnnotationName = name.substring(secondToLastDotIndex);

            createLegacyFile(newName, e.getValue());
        });


        return true;
    }

    private void createLegacyFile(String className, Definition definition) {
        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }

        String simpleClassName = className.substring(lastDot + 1);
        String annotationTypeName = definition.typeElement.getQualifiedName().toString();

        try {
            JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);

            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                out.printf ("package %s;\n\n", packageName);
            }

                out.println("import java.util.Map;");
                out.println("import org.osgi.service.component.ComponentContext;");
                out.println("import java.lang.annotation.Annotation;");
                out.println();

                out.printf ("class %s extends com.adobe.acs.commons.util.impl.backwardscompatability.AbstractLegacyConfig implements %s {\n",
                    simpleClassName, annotationTypeName);
                out.println();
                out.printf ("    private final %s config;\n", annotationTypeName);
                out.println();
                out.printf ("    public %s (%s config, Map<String, Object> legacy) {\n", simpleClassName, annotationTypeName);
                out.println("        super(legacy);");
                out.println("        this.config = config;");
                out.println("    }");
                out.println();
                out.printf ("    public %s(%s config, ComponentContext legacy) {\n", simpleClassName, annotationTypeName);
                out.println("        super(legacy);");
                out.println("        this.config = config;");
                out.println("    }");
                out.println();

            definition.methods.entrySet().forEach(me -> {
                String returnType = me.getKey().getReturnType().toString();
                String methodName = me.getKey().getSimpleName().toString();
                out.println("    @Override");
                out.printf ("    public %s %s() {\n", returnType, methodName);
            if (me.getValue() == null) {
                out.printf ("        return config.%s();\n", methodName);
            } else {
                out.printf ("        return legacy.get(\"%s\", config.%s());\n", me.getValue(), methodName);
            }
                out.println("    }");
                out.println();

            });
                out.println();
                out.println("    public Class<? extends Annotation> annotationType() {");
                out.printf ("        return %s.class;\n", annotationTypeName);
                out.println("    }");
                out.println("}");
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private class Definition {

        private final TypeElement typeElement;
        private final Map<ExecutableElement, String> methods;

        private Definition(TypeElement typeElement) {
            this.typeElement = typeElement;
            this.methods = new LinkedHashMap<>();
            typeElement.getEnclosedElements().stream().filter(e -> e instanceof ExecutableElement).forEach(e -> {
                this.methods.put((ExecutableElement) e, null);
            });
        }

        private void enableLegacy(ExecutableElement method, String legacyName) {
            methods.put(method, legacyName);
        }

    }
}