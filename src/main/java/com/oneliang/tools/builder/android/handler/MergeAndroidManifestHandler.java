package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.base.AndroidConfigurationForJson;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.tools.builder.base.KeyValue;
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
        @SuppressWarnings("unchecked")
        List<KeyValue<String, String>> keyValueList = (List<KeyValue<String, String>>) this.androidConfiguration.getTemporaryData(AndroidConfigurationForJson.TEMPORARY_DATA_MANIFEST_REPLACE_KEYWORD_LIST);
        if (keyValueList != null && !keyValueList.isEmpty()) {
            byte[] androidManifestContentByteArray = FileUtil.readFile(androidManifestOutput);
            try {
                String androidManifestContent = new String(androidManifestContentByteArray, Constant.Encoding.UTF8);
                for (KeyValue<String, String> keyValue : keyValueList) {
                    androidManifestContent = androidManifestContent.replace(keyValue.key, keyValue.value);
                }
                FileUtil.writeFile(androidManifestOutput, androidManifestContent.getBytes(Constant.Encoding.UTF8));
            } catch (Exception e) {
                logger.error("Android manifest replace keyword error:" + e.getMessage());
            }
        }

        return true;
    }
}
