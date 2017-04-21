package com.oneliang.tools.builder.android.handler;

import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.file.FileUtil;

public class GenerateApkHandler extends AbstractAndroidHandler {

    public boolean handle() {
        FileUtil.createDirectory(this.androidConfiguration.getMainAndroidProject().getPrepareOutput());
        FileUtil.createDirectory(this.androidConfiguration.getMainAndroidProject().getPrepareAssetsOutput());
        FileUtil.createDirectory(this.androidConfiguration.getMainAndroidProject().getPrepareLibOutput());
        boolean allAssetsFileHasCache = this.androidConfiguration.isAllAssetsFileHasNotChanged();
        boolean allCompileFileHasCache = isAllCompileFileHasCache(this.androidConfiguration.getAndroidProjectList());
        if (!allAssetsFileHasCache || !allCompileFileHasCache) {
            logger.info("All assets file has cache:" + allAssetsFileHasCache + ",all compile file has cache:" + allCompileFileHasCache);
            // copy all dex to publish prepare
            FileUtil.copyFile(this.androidConfiguration.getMainAndroidProject().getMergeDexOutput(), this.androidConfiguration.getMainAndroidProject().getPrepareOutput(), FileUtil.FileCopyType.PATH_TO_PATH);
            String apkOutputFullFilename = this.androidConfiguration.getMainAndroidProject().getUnsignedApkFullFilename();
            // BuilderUtil.androidApkBuilder(apkOutputFullFilename,
            // resourceFullFilename, dexFullFilename,
            // libDirectoryList,this.configuration.isApkDebug());
            BuilderUtil.generateApk(this.androidConfiguration.getMainAndroidProject().getPrepareOutput(), apkOutputFullFilename);
        }
        return true;
    }
}
