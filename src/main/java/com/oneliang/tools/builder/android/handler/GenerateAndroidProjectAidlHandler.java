package com.oneliang.tools.builder.android.handler;

import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.base.BuildException;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.tools.builder.base.CacheHandler.CacheOption.ChangedFileProcessor;
import com.oneliang.tools.builder.base.ChangedFile;

public class GenerateAndroidProjectAidlHandler extends AndroidProjectHandler {

    private boolean aidlCompileSuccess = false;

    public boolean handle() {
        List<String> sourceDirectoryList = this.androidProject.getSourceDirectoryList();// getSources();
        String aidlFileCacheFullFilename = this.androidProject.getCacheOutput() + Constant.Symbol.SLASH_LEFT + CACHE_AIDL_FILE;
        CacheOption cacheOption = new CacheOption(aidlFileCacheFullFilename, sourceDirectoryList);
        cacheOption.fileSuffix = Constant.Symbol.DOT + Constant.File.AIDL;
        cacheOption.changedFileProcessor = new ChangedFileProcessor() {
            public boolean process(Iterable<ChangedFile> changedFileIterable) {
                boolean result = false;
                if (changedFileIterable != null && changedFileIterable.iterator().hasNext()) {
                    try {
                        for (ChangedFile changedFile : changedFileIterable) {
                            if (changedFile.status.equals(ChangedFile.Status.DELETED)) {
                                continue;
                            }
                            BuilderUtil.executeAndroidAidl(android.getAidlExecutor(), android.findAndroidApiFrameworkAidl(androidProject.getCompileTarget()), changedFile.directory, androidProject.getGenOutput(), changedFile.fullFilename);
                        }
                        result = true;
                    } catch (Exception e) {
                        logger.error(Constant.Base.EXCEPTION, e);
                        throw new BuildException(e);
                    }
                } else {
                    result = true;
                }
                if (result) {
                    aidlCompileSuccess = true;
                }
                return result;
            }
        };
        this.dealWithCache(cacheOption);
        return this.aidlCompileSuccess;
    }
}
