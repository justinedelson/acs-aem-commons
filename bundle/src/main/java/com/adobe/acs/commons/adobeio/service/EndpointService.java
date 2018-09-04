/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.adobeio.service;

import aQute.bnd.annotation.ProviderType;

/**
 * This is the interface for the EndpointService.<br/>
 * Using this service, the calling component can post to and retrieve from the endpoint.<br/>
 * All the plumbing around authentication will be taken care of.<br/>
 *
 * Example how to use in the calling component:
 *   @Reference(target = "(id=&lt;put your id here&gt;)")<br/>
 *   private EndpointService endpointService;
 */
@ProviderType
public interface EndpointService extends Endpoint {

    /**
     * @return The ID of the endpoint
     */
    String getId();

    /**
     * This is a test for the connection to the endpoint.
     * The test will be performed using the URL and the GET-method.
     * @return TRUE if connection is successful
     */
    boolean isConnected();

    EndpointBuilder builder();

    interface EndpointBuilder {
        EndpointBuilder withUrl(String url);
        EndpointBuilder withMethod(String method);
        EndpointBuilder withServiceSpecificHeaders(String[] serviceSpecificHeaders, boolean merge);
        Endpoint build();
    }

}
