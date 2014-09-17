/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 Adobe
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
/*global CQ: false, ACS: false */
ACS.CQ.wcm.SHOW_DISABLED_SIDEKICK_BUTTON = false;

(function(){
    var init = function(sidekick) {
        var bbar = sidekick.getBottomToolbar(),
            button = new CQ.Ext.Button({
                id: "acs-commons-sidekick-wcmmode-disabled",
                iconCls: "acs-commons-sidekick-wcmmode-disabled",
                tooltip: {
                    title: "Switch",
                    text: "Switch to wcmmode=disabled"
                },
                handler: function() {
                     var url = CQ.WCM.getContentUrl();
                     if (url.indexOf("?")) {
                         url += "&";
                     } else {
                         url += "?";
                     }
                     url += "wcmmode=disabled";

                     CQ.WCM.getTopWindow().open(url, "_blank");
                }
            });
        bbar.insert(0, button);
    };

    CQ.WCM.on("sidekickready", function(sidekick) {
        sidekick.on("loadcontent", function() {
            if (ACS.CQ.wcm.SHOW_DISABLED_SIDEKICK_BUTTON) {
                init(sidekick);
            }
        });
    });
}());