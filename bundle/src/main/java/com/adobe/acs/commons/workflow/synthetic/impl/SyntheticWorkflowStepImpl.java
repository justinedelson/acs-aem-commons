/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

package com.adobe.acs.commons.workflow.synthetic.impl;

import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;

import java.util.Map;

public class SyntheticWorkflowStepImpl implements com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowStep {
    private final String id;
    private Map<String, Object> metadataMap;
    private SyntheticWorkflowRunner.WorkflowProcessIdType idType;


    public SyntheticWorkflowStepImpl(String id, Map<String, Object> metadataMap) {
        this.idType = SyntheticWorkflowRunner.WorkflowProcessIdType.PROCESS_NAME;
        this.id = id;
        this.metadataMap = metadataMap;
    }

    public SyntheticWorkflowStepImpl(String id, SyntheticWorkflowRunner.WorkflowProcessIdType type, Map<String, Object> metadataMap) {
        this.idType = type;
        this.id = id;
        this.metadataMap = metadataMap;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, Object> getMetadataMap() {
        return metadataMap;
    }

    @Override
    public SyntheticWorkflowRunner.WorkflowProcessIdType getIdType() {
        return idType;
    }
}
