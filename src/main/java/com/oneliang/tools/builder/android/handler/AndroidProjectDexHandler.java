package com.oneliang.tools.builder.android.handler;

import java.util.List;

import com.oneliang.Constants;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.file.FileUtil;

public class AndroidProjectDexHandler extends AndroidProjectHandler {

    public boolean handle() {
        boolean isAllCompileFileHasCache = androidProject.isAllCompileFileHasNotChanged();
        if (!isAllCompileFileHasCache || !this.androidConfiguration.isApkDebug()) {
            String dexOutputDirectory = androidProject.getDexOutput();
            FileUtil.createDirectory(dexOutputDirectory);
            String dexFullFilename = dexOutputDirectory + "/" + optimizeName(androidProject.getName()) + Constants.Symbol.DOT + Constants.File.DEX;
            List<String> classesJarListAndLibraryList = this.getAndroidProjectJarListWithProguard(androidProject);
            if (classesJarListAndLibraryList != null && !classesJarListAndLibraryList.isEmpty()) {
                BuilderUtil.androidDx(dexFullFilename, classesJarListAndLibraryList, this.androidConfiguration.isApkDebug());
            }
        }
        return true;
    }
}
