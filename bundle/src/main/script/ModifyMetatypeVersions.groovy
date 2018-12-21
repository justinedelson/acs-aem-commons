/*
 * #%L
 * ACS AEM Commons Content Package
 * %%
 * Copyright (C) 2014 Adobe
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

def copy(inputStream, outputStream) {
    def buffer = new byte[1024];

    def len;
    while ((len = inputStream.read(buffer)) > 0) {
        outputStream.write(buffer, 0, len);
    }
}

def destFile = new File(project.build.directory, "${project.artifactId}-${project.version}-tmp.jar");
def sourceFile = new File(project.build.directory, "${project.build.finalName}.jar");

def input = new java.util.zip.ZipInputStream(new FileInputStream(sourceFile));
def output = new java.util.zip.ZipOutputStream(new FileOutputStream(destFile));
 
def entry = input.getNextEntry();
while (entry != null) {
    def name = entry.getName();

    output.putNextEntry(new java.util.zip.ZipEntry(name));

    def isMetatype = name =~ /OSGI-INF\/metatype\//;
    if (isMetatype) {
        def buffer = new ByteArrayOutputStream();
        copy(input, buffer);
        def xmlContent = buffer.toString("UTF-8")
        xmlContent = xmlContent.replace("http://www.osgi.org/xmlns/metatype/v1.4.0", "http://www.osgi.org/xmlns/metatype/v1.2.0")
        copy(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")), output);
    } else {
        copy(input, output)
    }
    entry = input.getNextEntry();
}
input.close();
output.close();

destFile.renameTo(sourceFile)