package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.android.base.AndroidProject.DirectoryType;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.tools.builder.base.CacheHandler.CacheOption.ChangedFileProcessor;
import com.oneliang.tools.builder.base.ChangedFile;
import com.oneliang.util.file.FileUtil;

public class PackageResourceHandler extends AbstractAndroidHandler {

    public boolean handle() {
        final List<String> assetsDirectoryList = this.androidConfiguration.findDirectoryOfAndroidProjectList(this.androidConfiguration.getAndroidProjectList(), AndroidProject.DirectoryType.ASSETS);
        String assetsFileCacheFullFilename = this.androidConfiguration.getPublicAndroidProject().getCacheOutput() + "/" + CACHE_ASSETS_FILE;
        CacheOption cacheOption = new CacheOption(assetsFileCacheFullFilename, assetsDirectoryList);
        cacheOption.changedFileProcessor = new ChangedFileProcessor() {
            public boolean process(Iterable<ChangedFile> changedFileIterable) {
                String prepareOutput = androidConfiguration.getMainAndroidProject().getPrepareAssetsOutput();
                FileUtil.createDirectory(prepareOutput);
                if (changedFileIterable != null && changedFileIterable.iterator().hasNext()) {
                    Iterator<ChangedFile> changedFileIterator = changedFileIterable.iterator();
                    while (changedFileIterator.hasNext()) {
                        ChangedFile changedFile = changedFileIterator.next();
                        if (changedFile.status.equals(ChangedFile.Status.DELETED)) {
                            continue;
                        }
                        String directory = changedFile.directory;
                        String fullFilename = changedFile.fullFilename;
                        String from = fullFilename;
                        String relativePath = new File(fullFilename).getAbsolutePath().substring(new File(directory).getAbsolutePath().length() + 1);
                        String to = prepareOutput + Constant.Symbol.SLASH_LEFT + relativePath;
                        FileUtil.copyFile(from, to, FileUtil.FileCopyType.FILE_TO_FILE);
                    }
                } else {
                    androidConfiguration.setAllAssetsFileHasCache(true);
                }
                return true;
            }
        };
        this.dealWithCache(cacheOption);

        if (!this.androidConfiguration.isAllResourceFileHasCache() || !this.androidConfiguration.isAllAssetsFileHasCache()) {
            String androidManifest = this.androidConfiguration.getPublicAndroidProject().getAndroidManifestOutput();
            List<String> assetsList = new ArrayList<String>();
            assetsList.add(this.androidConfiguration.getMainAndroidProject().getPrepareAssetsOutput());
            String mergeResourceOutput = this.androidConfiguration.getMainAndroidProject().getMergeResourceOutput();
            FileUtil.createDirectory(mergeResourceOutput);
            String resourceFullFilename = mergeResourceOutput + "/" + AndroidProject.RESOURCE_FILE;
            List<String> resourceDirectoryList = new ArrayList<String>(this.androidConfiguration.findDirectoryOfAndroidProjectList(this.androidConfiguration.getAndroidProjectList(), DirectoryType.RES));
            if (FileUtil.isExist(this.androidConfiguration.getPublicRAndroidProject().getResourceOutput())) {
                resourceDirectoryList.add(this.androidConfiguration.getPublicRAndroidProject().getResourceOutput());
            }
            int result = BuilderUtil.executeAndroidAaptToPackageResource(this.android.getAaptExecutor(), androidManifest, resourceDirectoryList, assetsList, Arrays.asList(this.androidConfiguration.getMainAndroidApiJar()), resourceFullFilename, this.androidConfiguration.isApkDebug());
            logger.info("Aapt package resource result code:" + result);
            // unzip resources.ap_
            if (FileUtil.isExist(resourceFullFilename)) {
                FileUtil.unzip(resourceFullFilename, this.androidConfiguration.getMainAndroidProject().getPrepareOutput(), null);
            }
        }
        return true;
    }

}
