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
        List<String> maybeDuplicateAssetsDirectoryList = this.androidConfiguration.findDirectoryOfAndroidProjectList(this.androidConfiguration.getAndroidProjectList(), AndroidProject.DirectoryType.ASSETS);
        final List<String> assetsDirectoryList = this.filterDuplicateFile(maybeDuplicateAssetsDirectoryList);
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
                    androidConfiguration.setAllAssetsFileHasNotChanged(true);
                }
                return true;
            }
        };
        this.dealWithCache(cacheOption);

        List<String> maybeDuplicateResourceDirectoryList = new ArrayList<String>(this.androidConfiguration.findDirectoryOfAndroidProjectList(this.androidConfiguration.getAndroidProjectList(), DirectoryType.RES));
        final List<String> resourceDirectoryList = this.filterDuplicateFile(maybeDuplicateResourceDirectoryList);
        String resourceFileCacheFullFilename = this.androidConfiguration.getPublicAndroidProject().getCacheOutput() + "/" + CACHE_RESOURCE_FILE;
        cacheOption = new CacheOption(resourceFileCacheFullFilename, resourceDirectoryList);
        cacheOption.changedFileProcessor = new CacheOption.ChangedFileProcessor() {
            public boolean process(Iterable<ChangedFile> changedFileIterable) {
                boolean saveCache = false;
                if (changedFileIterable != null && changedFileIterable.iterator().hasNext()) {
                    String androidManifest = androidConfiguration.getPublicAndroidProject().getAndroidManifestOutput();
                    List<String> assetsList = new ArrayList<String>();
                    assetsList.add(androidConfiguration.getMainAndroidProject().getPrepareAssetsOutput());
                    String mergeResourceOutput = androidConfiguration.getMainAndroidProject().getMergeResourceOutput();
                    FileUtil.createDirectory(mergeResourceOutput);
                    String resourceFullFilename = mergeResourceOutput + "/" + AndroidProject.RESOURCE_FILE;

                    //add ids.xml and public.xml
                    if (FileUtil.isExist(androidConfiguration.getPublicRAndroidProject().getResourceOutput())) {
                        resourceDirectoryList.add(androidConfiguration.getPublicRAndroidProject().getResourceOutput());
                    }
                    
                    int result = BuilderUtil.executeAndroidAaptToPackageResource(android.getAaptExecutor(), androidManifest, resourceDirectoryList, assetsList, Arrays.asList(androidConfiguration.getMainAndroidApiJar()), resourceFullFilename, androidConfiguration.isApkDebug());
                    logger.info("Aapt package resource result code:" + result);
                    // unzip resources.ap_
                    if (FileUtil.isExist(resourceFullFilename)) {
                        FileUtil.unzip(resourceFullFilename, androidConfiguration.getMainAndroidProject().getPrepareOutput(), null);
                    }
                    if (result == 0) {
                        saveCache = true;
                    }
                } else {
                    androidConfiguration.setAllResourceFileHasNotChanged(true);
                }
                return saveCache;
            }
        };
        this.dealWithCache(cacheOption);
        return true;
    }

}
