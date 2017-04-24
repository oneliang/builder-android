package com.oneliang.tools.builder.android.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.base.AndroidProject.DirectoryType;
import com.oneliang.tools.builder.android.base.PublicAndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.tools.builder.base.ChangedFile;
import com.oneliang.util.file.FileUtil;

public class GeneratePublicRDotTxtHandler extends AbstractAndroidHandler {

    private static final String GEN_R_CACHE = "gen_R_cache";

    private boolean generateRSuccess = false;

    public boolean handle() {
        List<String> maybeDuplicateResourceDirectoryList = new ArrayList<String>(this.androidConfiguration.findDirectoryOfAndroidProjectList(this.androidConfiguration.getAndroidProjectList(), DirectoryType.RES));
        final List<String> resourceDirectoryList = this.filterDuplicateFile(maybeDuplicateResourceDirectoryList);
        String publicOutput = this.androidConfiguration.getPublicAndroidProject().getOutputHome();
        FileUtil.createDirectory(publicOutput);
        String resourceFileCacheFullFilename = this.androidConfiguration.getPublicAndroidProject().getCacheOutput() + "/" + GEN_R_CACHE;
        String publicRDotTxt = publicOutput + Constant.Symbol.SLASH_LEFT + PublicAndroidProject.R_TXT;
        CacheOption cacheOption = new CacheOption(resourceFileCacheFullFilename, resourceDirectoryList);
        cacheOption.changedFileProcessor = new CacheOption.ChangedFileProcessor() {
            public boolean process(Iterable<ChangedFile> changedFileIterable) {
                boolean saveCache = false;
                if (changedFileIterable != null && changedFileIterable.iterator().hasNext()) {
                    String mergedAndroidManifestOutput = androidConfiguration.getPublicAndroidProject().getAndroidManifestOutput();
                    int result = BuilderUtil.executeAndroidAaptToGenerateR(android.getAaptExecutor(), mergedAndroidManifestOutput, resourceDirectoryList, publicOutput, Arrays.asList(androidConfiguration.getMainAndroidApiJar()), androidConfiguration.isApkDebug());
                    logger.info("Aapt to generate r result code:" + result);
                    if (result == 0) {
                        saveCache = true;
                        generateRSuccess = true;
                    }
                } else {
                    androidConfiguration.setAllResourceFileHasNotChanged(true);
                    generateRSuccess = true;
                }
                return saveCache;
            }
        };
        this.dealWithCache(cacheOption);

        if (FileUtil.isExist(publicRDotTxt)) {
            androidConfiguration.setApkPatchInputRTxt(publicRDotTxt);
        }
        return generateRSuccess;
    }
}
