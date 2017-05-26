package com.oneliang.tools.builder.android.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.base.BuildException;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.tools.builder.base.Cache;
import com.oneliang.tools.builder.base.CacheHandler.CacheOption.ChangedFileProcessor;
import com.oneliang.tools.builder.base.ChangedFile;
import com.oneliang.tools.builder.base.KeyValue;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class CompileAndroidProjectHandler extends AndroidProjectHandler {

    private boolean compileSuccess = false;
    private boolean allCompileFileHasNotChanged = false;

    public boolean handle() {
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
                        for (String compileClasspath : compileClasspathList) {
                            logger.debug(androidProject.getName() + "," + compileClasspath);
                        }
                        javacResult = BuilderUtil.javac(togetherSourceList, classesOutput, androidConfiguration.isApkDebug(), null, androidProject.getCompileProcessorPathList(), compileClasspathList, Arrays.asList(new KeyValue<String, String>("-source", "1.7"), new KeyValue<String, String>("-target", "1.7")));
                        // javacResult =
                        // BuilderUtil.executeJavac(java.getJavacExecutor(),
                        // compileClasspathList, togetherSourceList,
                        // classesOutput, androidConfiguration.isApkDebug(),
                        // androidProject.getCompileProcessorPathList());
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
        final Cache javaFileCache = this.dealWithCache(cacheOption);
        if (!this.compileSuccess) {
            return false;
        }
        // jar file cache
        String jarFileCacheFullFilename = androidProject.getCacheOutput() + Constant.Symbol.SLASH_LEFT + CACHE_JAR_FILE;
        cacheOption = new CacheOption(jarFileCacheFullFilename, this.androidProject.getLibsDirectoryList());
        cacheOption.fileSuffix = Constant.Symbol.DOT + Constant.File.JAR;
        cacheOption.changedFileProcessor = new CacheOption.ChangedFileProcessor() {
            public boolean process(Iterable<ChangedFile> changedFileIterable) {
                if ((javaFileCache != null && (!javaFileCache.changedFileMap.isEmpty())) || (changedFileIterable != null && (changedFileIterable.iterator().hasNext()))) {
                    allCompileFileHasNotChanged = false;
                } else {
                    allCompileFileHasNotChanged = true;
                    // R.java updated so need to merge to main dex
                    if (androidProject.getName().equals(androidConfiguration.getProjectMain())) {
                        if (!androidConfiguration.isAllResourceFileHasNotChanged()) {
                            allCompileFileHasNotChanged = false;
                        }
                    }
                }
                return true;
            }
        };
        this.dealWithCache(cacheOption);
        this.androidProject.setAllCompileFileHasNotChanged(allCompileFileHasNotChanged);
        return true;
    }
}
