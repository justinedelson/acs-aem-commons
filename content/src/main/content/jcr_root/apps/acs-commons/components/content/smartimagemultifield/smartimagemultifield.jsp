<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 Adobe
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
  --%>
<%@include file="/libs/foundation/global.jsp"%>

<%@ page import="java.util.Iterator" %>
<%@ page import="com.day.cq.wcm.foundation.Image" %>
<%@ page import="org.apache.sling.commons.json.JSONArray" %>

<%
    Iterator<Resource> children = resource.listChildren();

    if(!children.hasNext()){
%>
        <br><br>

        Double-Click to add Images

        <br><br>
<%
    }else{
        Resource imagesResource = children.next();
        ValueMap map = imagesResource.adaptTo(ValueMap.class);
        String order = map.get("order", String.class);

        Image img = null; String src = null;
        JSONArray array = new JSONArray(order);

        for(int i = 0; i < array.length(); i++){
            img = new Image(resource);
            img.setItemName(Image.PN_REFERENCE, "imageReference");
            img.setSuffix(String.valueOf(array.get(i)));
            img.setSelector("img");

            src = img.getSrc();
%>
            <img src='<%=src%>'/><br><br>
<%
        }
    }
%>