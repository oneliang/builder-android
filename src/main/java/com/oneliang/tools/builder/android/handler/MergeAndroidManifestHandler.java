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
                if (sourceDirectoryFile.getName().equals("src")) {
                    String androidManifest = new File(sourceDirectoryFile.getParent(), "AndroidManifest.xml").getAbsolutePath();
                    if (FileUtil.isExist(androidManifest)) {
                        allAndroidManifestList.add(androidManifest);
                    }
                }
            }
        }
        logger.info("all android manifest size:" + allAndroidManifestList.size());
        String androidManifestOutput = this.androidConfiguration.getPublicAndroidProject().getAndroidManifestOutput();
        FileUtil.createFile(androidManifestOutput);
        BuilderUtil.mergeAndroidManifest(this.androidConfiguration.getPackageName(), allAndroidManifestList, androidManifestOutput, true);// this.androidConfiguration.isApkDebug());
        return true;
    }
}
