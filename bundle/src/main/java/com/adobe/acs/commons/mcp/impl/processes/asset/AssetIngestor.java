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
package com.adobe.acs.commons.mcp.impl.processes.asset;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.mcp.model.FieldFormat;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.mcp.model.ValueFormat;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AssetIngestor extends ProcessDefinition {

    public static final String ALL_ASSETS = "All Assets";
    static final String[] AUTHORIZED_GROUPS = new String[]{
            "administrators",
            "asset-ingest",
            "dam-administrators"
    };

    protected final transient MimeTypeService mimetypeService;

    @SuppressWarnings("squid:S00115")
    public static enum AssetAction {
        skip, version, replace
    }

    public AssetIngestor(MimeTypeService mimeTypeService) {
        this.mimetypeService = mimeTypeService;
        this.reportRows = Collections.synchronizedList(new ArrayList<>());
        this.createdFolders = trackActivity("All folders", "Create", "Count of all folders created", 0L);
        this.importedAssets = trackActivity(ALL_ASSETS, "Import", "Count of all assets imports", 0L);
        this.skippedFiles = trackActivity(ALL_ASSETS, "Skipped", "Count of skipped files", 0L);
        this.importedData = trackActivity(ALL_ASSETS, "Data imported", "Count of bytes imported", 0L);
    }

    @FormField(
            name = "Dry run",
            description = "If checked, no import happens.  Useful for data validation",
            component = CheckboxComponent.class
    )
    boolean dryRunMode = false;

    @FormField(
            name = "Detailed report",
            description = "If checked, information about every asset is recorded",
            component = CheckboxComponent.class,
            options = "checked"
    )
    boolean detailedReport = true;

    @FormField(
            name = "Inhibit workflow",
            description = "If checked, disables asset processing workflow",
            component = CheckboxComponent.class,
            options = "checked"
    )
    boolean inhibitWorkflow = true;

    @FormField(
            name = "Preserve Filename",
            description = "If checked, file name is preserved as asset name.  If unchecked, asset name is converted to a JCR-friendly name.",
            component = CheckboxComponent.class,
            options = "checked"
    )
    boolean preserveFileName = true;

    @FormField(
            name = "Target JCR Folder",
            description = "Prepended to target path if it does not begin with /content",
            hint = "/content/dam",
            component = PathfieldComponent.FolderSelectComponent.class,
            required = true,
            options = {"default=/content/dam", "base=/content/dam"}
    )
    String jcrBasePath = "/content/dam";
    @FormField(
            name = "Ignore folders",
            description = "List of folder names to be ignored",
            hint = "tmp,.DS_STORE",
            options = {"default=tmp,ds_store,.ds_store,.thumbs,.appledouble"}
    )
    String ignoreFolders = "tmp,ds_store,.ds_store,.thumbs,.appledouble";
    List<String> ignoreFolderList;

    @FormField(
            name = "Ignore files",
            description = "List of file names to ignore",
            hint = "full file names, comma separated",
            options = {"default=ds_store,.ds_store"}
    )
    String ignoreFiles = "ds_store,.ds_store";
    List<String> ignoreFileList;

    @FormField(
            name = "Ignore extensions",
            description = "List of file extensions to ignore",
            hint = "mp4,txt, etc.",
            options = {"default=txt,html,css,js,thm,exe,db"}
    )
    String ignoreExtensions = "txt,html,css,js,thm,exe,db";
    List<String> ignoreExtensionList;

    @FormField(
            name = "Existing action",
            description = "What to do if an asset exists",
            component = RadioComponent.EnumerationSelector.class,
            options = {"default=skip", "vertical"}
    )
    protected transient AssetAction existingAssetAction = AssetAction.skip;
    @FormField(
            name = "Minimum size",
            description = "Min size to import (in bytes), 0=none",
            hint = "1024...",
            options = {"default=1024"}
    )
    private transient long minimumSize = 1024;
    @FormField(
            name = "Maximum size",
            description = "Max size to import (in bytes), 0=none",
            hint = "1gb = 1073741824",
            options = {"default=1073741824"}
    )
    private transient long maximumSize = 1073741824;

    protected static final String DEFAULT_FOLDER_TYPE = "sling:Folder";
    protected static final String CHANGED_BY_WORKFLOW = "changedByWorkflowProcess";

    final Map<ReportColumns, Object> createdFolders;
    final Map<ReportColumns, Object> importedAssets;
    final Map<ReportColumns, Object> skippedFiles;
    final Map<ReportColumns, Object> importedData;

    @SuppressWarnings("squid:S00115")
    public static enum ReportColumns {
        item, action, description, count, @FieldFormat(ValueFormat.storageSize) bytes
    }

    final List<Map<ReportColumns, Object>> reportRows;

    private Map<ReportColumns, Object> trackActivity(String item, String action, String description, Long bytes) {
        Map<ReportColumns, Object> reportRow = Collections.synchronizedMap(new EnumMap<>(ReportColumns.class));
        reportRow.put(ReportColumns.item, item);
        reportRow.put(ReportColumns.action, action);
        reportRow.put(ReportColumns.description, description);
        reportRow.put(ReportColumns.count, 0L);
        reportRow.put(ReportColumns.bytes, bytes);
        reportRows.add(reportRow);
        return reportRow;
    }

    protected Map<ReportColumns, Object> trackDetailedActivity(String item, String action, String description, Long bytes) {
        if (detailedReport) {
            return trackActivity(item, action, description, bytes);
        } else {
            return null;
        }
    }

    @SuppressWarnings("squid:S2445")
    private void increment(Map<ReportColumns, Object> row, ReportColumns col, long amt) {
        if (row != null) {
            synchronized (row) {
                row.put(col, (Long) row.getOrDefault(col, 0) + amt);
            }
        }
    }

    protected void incrementCount(Map<ReportColumns, Object> row, long amt) {
        increment(row, ReportColumns.count, amt);
    }

    protected void incrementBytes(Map<ReportColumns, Object> row, long amt) {
        increment(row, ReportColumns.bytes, amt);
    }

    protected long getCount(Map<ReportColumns, Object> row) {
        return (long) row.getOrDefault(ReportColumns.count, 0);
    }

    @Override
    public void init() throws RepositoryException {
        if (ignoreFolders == null) {
            ignoreFolders = "";
        }
        ignoreFolderList = Arrays.asList(ignoreFolders.trim().toLowerCase().split(","));
        if (ignoreFiles == null) {
            ignoreFiles = "";
        }
        ignoreFileList = Arrays.asList(ignoreFiles.trim().toLowerCase().split(","));
        if (ignoreExtensions == null) {
            ignoreExtensions = "";
        }
        ignoreExtensionList = Arrays.asList(ignoreExtensions.trim().toLowerCase().split(","));
    }

    @SuppressWarnings("squid:S00112")
    private void createAsset(Source source, String assetPath, ResourceResolver r, boolean versioning) throws Exception {
        try {
            boolean versioned = false;
            if (!dryRunMode) {
                disableWorkflowProcessing(r);
                AssetManager assetManager = r.adaptTo(AssetManager.class);
                String type = mimetypeService.getMimeType(source.getName());
                if (versioning) {
                    //if asset is null, no version gets created
                    Asset asset = r.getResource(assetPath).adaptTo(Asset.class);
                    versioned = asset != null;
                    //once you are past this first version, default behavior is to start numbering 1.0, 1.1 and so on
                    assetManager.createRevision(asset, "initial version of asset", asset.getName());
                    r.commit();
                    r.refresh();
                    //once version is committed we are safe to create, which only replaces the original version
                }
                assetManager.createAsset(assetPath, source.getStream(), type, false);
                r.commit();
                r.refresh();
            }
            if (versioned) {
                trackDetailedActivity(assetPath, "Revised", "Created new version of asset", source.getLength());
            } else {
                trackDetailedActivity(assetPath, "Create", "Imported asset", source.getLength());
            }
            incrementBytes(importedData, source.getLength());
            incrementCount(importedAssets, 1L);
        } finally {
            source.close();
        }
    }

    protected void handleExistingAsset(Source source, String assetPath, ResourceResolver r) throws Exception {
        switch (existingAssetAction) {
            case skip:
                //if skip then we only create asset if it doesn't exist
                if (r.getResource(assetPath) == null) {
                    createAsset(source, assetPath, r, false);
                } else {
                    incrementCount(skippedFiles, 1L);
                    trackDetailedActivity(assetPath, "Skip", "Skipped existing asset", 0L);
                }
                break;
            case replace:
                //if replace we just create a new one and the old one goes away
                createAsset(source, assetPath, r, false);
                break;
            default:
                //only option left is replace, we'll save current version as a version and then replace it
                versionExistingAsset(source, assetPath, r);
        }
    }

    @SuppressWarnings("squid:S3776")
    protected boolean createFolderNode(HierarchialElement el, ResourceResolver r) throws RepositoryException, PersistenceException {
        if (el == null || !el.isFolder()) {
            return false;
        }
        if (dryRunMode) {
            return true;
        }
        String folderPath = el.getNodePath();
        String name = el.getName();
        Session s = r.adaptTo(Session.class);
        if (s.nodeExists(folderPath)) {
            Node folderNode = s.getNode(folderPath);
            Node folderContentNode = folderNode.hasNode(JcrConstants.JCR_CONTENT) ? folderNode.getNode(JcrConstants.JCR_CONTENT) : null;
            if (folderNode.getPath().equals(jcrBasePath) || (null != folderContentNode
                    && folderContentNode.hasProperty(JcrConstants.JCR_TITLE)
                    && folderContentNode.getProperty(JcrConstants.JCR_TITLE).getString().equals(name))) {
                return false;
            } else {
                if (folderContentNode == null) {
                    folderContentNode = folderNode.addNode(JcrConstants.JCR_CONTENT,JcrConstants.NT_UNSTRUCTURED);
                }
                folderContentNode.setProperty(JcrConstants.JCR_TITLE, name);
                r.commit();
                r.refresh();
                return true;
            }
        }
        HierarchialElement parent = el.getParent();
        String parentPath;
        if (parent == null) {
            parentPath = jcrBasePath;
        } else {
            parentPath = parent.getNodePath();
        }
        if (!jcrBasePath.equals(parentPath)) {
            createFolderNode(parent, r);
        }
        Node child = s.getNode(parentPath).addNode(el.getNodeName(), DEFAULT_FOLDER_TYPE);
        trackDetailedActivity(el.getNodePath(), "Create Folder", "Create folder", 0L);
        setFolderTitle(child,folderPath,name);
        incrementCount(createdFolders, 1L);
        r.commit();
        r.refresh();
        return true;
    }

    private void setFolderTitle(Node child,String folderPath,String title) throws RepositoryException{
        if (!folderPath.equals(jcrBasePath)) {
            if(child.hasNode(JcrConstants.JCR_CONTENT)){
                child.getNode(JcrConstants.JCR_CONTENT).setProperty(JcrConstants.JCR_TITLE, title);
            }else{
                child.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_UNSTRUCTURED).setProperty(JcrConstants.JCR_TITLE, title);
            }
        }
    }

    @SuppressWarnings("squid:S00112")
    private void versionExistingAsset(Source source, String assetPath, ResourceResolver r) throws Exception {
        createAsset(source, assetPath, r, r.getResource(assetPath) != null);
    }

    protected CheckedConsumer<ResourceResolver> importAsset(final Source source, ActionManager actionManager) {
        return (ResourceResolver r) -> {
            createFolderNode(source.getElement().getParent(), r);
            actionManager.setCurrentItem(source.getElement().getItemName());
            HierarchialElement el = source.getElement();
            String path = source.getElement().getNodePath();
            if(null != el && el.isFile() && el.getName().contains(".") && !preserveFileName){
                String baseName = StringUtils.substringBeforeLast(el.getName(), ".");
                String extension = StringUtils.substringAfterLast(el.getName(), ".");
                path = (el.getParent() == null ? el.getJcrBasePath() : el.getParent().getNodePath()) + "/"
                        + JcrUtil.createValidName(baseName,JcrUtil.HYPHEN_LABEL_CHAR_MAPPING,"-")
                        + "." + JcrUtil.createValidName(extension,JcrUtil.HYPHEN_LABEL_CHAR_MAPPING,"-");
            }
            handleExistingAsset(source, path, r);
        };
    }

    protected boolean canImportFile(Source source) throws IOException {
        String name = source.getName().toLowerCase();
        if (minimumSize > 0 && source.getLength() < minimumSize) {
            return false;
        }
        if (maximumSize > 0 && source.getLength() > maximumSize) {
            return false;
        }
        if (name.startsWith(".") || ignoreFileList.contains(name)) {
            return false;
        }
        if (name.contains(".")) {
            int extPos = name.lastIndexOf('.');
            String ext = name.substring(extPos + 1);
            if (ignoreExtensionList.contains(ext)) {
                return false;
            }
        }
        return true;
    }

    protected boolean canImportFolder(HierarchialElement element) {
        String name = element.getName();
        if (ignoreFolderList.contains(name.toLowerCase())) {
            return false;
        } else {
            HierarchialElement parent = element.getParent();
            if (parent == null) {
                return true;
            } else {
                return canImportFolder(parent);
            }
        }
    }

    protected boolean canImportContainingFolder(HierarchialElement element) {
        HierarchialElement parent = element.getParent();
        if (parent == null) {
            return true;
        } else {
            return canImportFolder(parent);
        }
    }

    protected void disableWorkflowProcessing(ResourceResolver rr) throws RepositoryException {
        if (inhibitWorkflow) {
            rr.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData(CHANGED_BY_WORKFLOW);
        }
    }

    private transient GenericReport report = new GenericReport();

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        report.setRows(reportRows.stream().map(EnumMap::new).collect(Collectors.toList()), ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    protected interface Source {

        String getName();

        InputStream getStream() throws IOException;

        long getLength() throws IOException;

        HierarchialElement getElement();

        void close() throws IOException;

    }

    protected interface HierarchialElement {

        boolean isFile();

        boolean isFolder();

        HierarchialElement getParent();

        String getName();

        String getItemName();

        Source getSource();

        String getJcrBasePath();

        default String getNodePath() {
            HierarchialElement parent = getParent();
            return (parent == null ? getJcrBasePath() : parent.getNodePath()) + "/" + getNodeName();
        }

        default String getNodeName() {
            String name = getName();
            if (isFile() && name.contains(".")) {
                return name;
            } else {
                return JcrUtil.createValidName(name,JcrUtil.HYPHEN_LABEL_CHAR_MAPPING,"-");
            }
        }
    }
}