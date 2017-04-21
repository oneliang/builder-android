package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.file.FileUtil;

public class MergeAndroidManifestHandler extends AbstractAndroidHandler {

    public boolean handle() {
        List<String> allAndroidManifestList = new ArrayList<String>();
        for (AndroidProject androidProject : this.androidConfiguration.getAndroidProjectList()) {
            // if(!this.androidConfiguration.getProjectMain().equals(androidProject.getName())){
            allAndroidManifestList.addAll(androidProject.getAndroidManifestList());
            // }
            for (String sourceDirectory : androidProject.getSourceDirectoryList()) {
                File sourceDirectoryFile = new File(sourceDirectory);
                if (!sourceDirectoryFile.getName().equals("src")) {
                    continue;
                }
                String androidManifest = new File(sourceDirectoryFile.getParent(), "AndroidManifest.xml").getAbsolutePath();
                if (FileUtil.isExist(androidManifest)) {
                    allAndroidManifestList.add(androidManifest);
                }
            }
        }
        Object aarAndroidManifestXmlListObject = this.androidConfiguration.getTemporaryData(InitializeAndroidProjectForGradleHandler.TEMPORARY_DATA_AAR_ANDROID_MANIFEST_XML_LIST);
        if (aarAndroidManifestXmlListObject != null && (aarAndroidManifestXmlListObject instanceof List)) {
            @SuppressWarnings("unchecked")
            List<String> aarAndroidManifestXmlList = (List<String>) aarAndroidManifestXmlListObject;
            allAndroidManifestList.addAll(aarAndroidManifestXmlList);
        }
        allAndroidManifestList = this.filterDuplicateFile(allAndroidManifestList);
        logger.info("all android manifest size:" + allAndroidManifestList.size());
        String androidManifestOutput = this.androidConfiguration.getPublicAndroidProject().getAndroidManifestOutput();
        FileUtil.createFile(androidManifestOutput);
        BuilderUtil.mergeAndroidManifest(this.androidConfiguration.getPackageName(), this.androidConfiguration.getMinSdkVersion(), this.androidConfiguration.getTargetSdkVersion(), allAndroidManifestList, androidManifestOutput, this.androidConfiguration.isApkDebug());
        return true;
    }
}
