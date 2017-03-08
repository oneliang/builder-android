package com.oneliang.tools.builder.android.handler;

import java.util.ArrayList;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.base.BuildException;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.tools.builder.base.Cache;
import com.oneliang.tools.builder.base.CacheHandler.CacheOption.ChangedFileProcessor;
import com.oneliang.tools.builder.base.ChangedFile;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class CompileAndroidProjectHandler extends AndroidProjectHandler {

    private boolean compileSuccess = false;

    public boolean handle() {
        boolean allCompileFileHasCache = false;
        final List<String> sourceFileDirectoryList = androidProject.getSourceDirectoryList();
        String sourceFileCacheFullFilename = androidProject.getCacheOutput() + Constant.Symbol.SLASH_LEFT + CACHE_JAVA_FILE;
        CacheOption cacheOption = new CacheOption(sourceFileCacheFullFilename, sourceFileDirectoryList);
        cacheOption.fileSuffix = Constant.Symbol.DOT + Constant.File.JAVA;
        cacheOption.changedFileProcessor = new ChangedFileProcessor() {
            public boolean process(Iterable<ChangedFile> changedFileIterable) {
                boolean result = false;
                String classesOutput = androidProject.getClassesOutput();
                List<String> classpathList = androidProject.getCompileClasspathList();
                if (changedFileIterable != null && changedFileIterable.iterator().hasNext()) {
                    List<String> togetherSourceList = new ArrayList<String>();
                    String javacSourceListFullFilename = androidProject.getCacheOutput() + Constant.Symbol.SLASH_LEFT + JAVAC_SOURCE_FILE_LIST;
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (ChangedFile changedFile : changedFileIterable) {
                            if (changedFile.status.equals(ChangedFile.Status.DELETED)) {
                                continue;
                            }
                            String source = changedFile.fullFilename;
                            stringBuilder.append(source);
                            if (BuilderUtil.isWindowsOS()) {
                                stringBuilder.append(StringUtil.CRLF_STRING);
                            } else {
                                stringBuilder.append(StringUtil.LF_STRING);
                            }
                            // logger.log("\t"+javaProject.getName()+" compile
                            // source file:"+source);
                        }
                        FileUtil.writeFile(javacSourceListFullFilename, stringBuilder.toString().getBytes(Constant.Encoding.UTF8));
                    } catch (Exception e) {
                        logger.error(Constant.Base.EXCEPTION, e);
                        throw new BuildException(e);
                    }
                    togetherSourceList.add(Constant.Symbol.AT + javacSourceListFullFilename);
                    List<String> compileClasspathList = new ArrayList<String>();
                    compileClasspathList.addAll(classpathList);
                    List<String> onlyCompileClasspathList = androidProject.getOnlyCompileClasspathList();
                    if (onlyCompileClasspathList != null) {
                        compileClasspathList.addAll(onlyCompileClasspathList);
                    }
                    int javacResult = 0;
                    try {
                        javacResult = BuilderUtil.javac(compileClasspathList, togetherSourceList, classesOutput, androidConfiguration.isApkDebug());
                    } catch (Throwable e) {
                        logger.error(androidProject.getName() + Constant.Symbol.COLON + Constant.Base.EXCEPTION, e);
                        javacResult = 1;
                    }
                    if (javacResult != 0) {
                        result = false;
                    } else {
                        result = true;
                    }
                } else {
                    result = true;
                }
                if (result) {
                    compileSuccess = true;
                }
                return result;
            }
        };
        Cache javaFileCache = this.dealWithCache(cacheOption);
        if (!this.compileSuccess) {
            return false;
        }
        // jar file cache
        String jarFileCacheFullFilename = androidProject.getCacheOutput() + Constant.Symbol.SLASH_LEFT + CACHE_JAR_FILE;
        cacheOption = new CacheOption(jarFileCacheFullFilename, this.androidProject.getLibsDirectoryList());
        cacheOption.fileSuffix = Constant.Symbol.DOT + Constant.File.JAR;
        Cache jarFileCache = this.dealWithCache(cacheOption);
        if ((javaFileCache != null && (!javaFileCache.changedFileMap.isEmpty())) || (jarFileCache != null && (!jarFileCache.changedFileMap.isEmpty()))) {
            allCompileFileHasCache = false;
        } else {
            allCompileFileHasCache = true;
            // R.java updated so need to merge to main dex
            if (this.androidProject.getName().equals(this.androidConfiguration.getProjectMain())) {
                if (!this.androidConfiguration.isAllResourceFileHasCache()) {
                    allCompileFileHasCache = false;
                }
            }
        }
        this.androidProject.setAllCompileFileHasCache(allCompileFileHasCache);
        return true;
    }
}
