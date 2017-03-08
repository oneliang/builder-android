package com.oneliang.tools.builder.android.handler;

import java.util.ArrayList;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.base.PublicAndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.tools.builder.base.CacheHandler.CacheOption.ChangedFileProcessor;
import com.oneliang.tools.builder.base.ChangedFile;
import com.oneliang.util.file.FileUtil;

public class CompilePublicRFileHandler extends AbstractAndroidHandler {

    public boolean handle() {
        if (!this.androidConfiguration.isAaptGenerateRFile()) {
            return true;
        }
        FileUtil.MatchOption matchOption = new FileUtil.MatchOption(this.androidConfiguration.getPublicRAndroidProject().getGenOutput());
        matchOption.fileSuffix = Constant.Symbol.DOT + Constant.File.JAVA;
        List<String> sourceList = FileUtil.findMatchFile(matchOption);
        String javaCacheFullFilename = this.androidConfiguration.getPublicRAndroidProject().getCacheOutput() + Constant.Symbol.SLASH_LEFT + CACHE_JAVA_FILE;
        CacheOption cacheOption = new CacheOption(javaCacheFullFilename, sourceList);
        cacheOption.fileSuffix = Constant.Symbol.DOT + Constant.File.JAVA;
        cacheOption.changedFileProcessor = new ChangedFileProcessor() {
            public boolean process(Iterable<ChangedFile> changedFileIterable) {
                if (changedFileIterable != null && changedFileIterable.iterator().hasNext()) {
                    List<String> sourceList = new ArrayList<String>();
                    for (ChangedFile changedFile : changedFileIterable) {
                        if (changedFile.status.equals(ChangedFile.Status.DELETED)) {
                            continue;
                        }
                        sourceList.add(changedFile.fullFilename);
                    }
                    List<String> classpathList = new ArrayList<String>();
                    classpathList.add(androidConfiguration.getMainAndroidApiJar());
                    classpathList.add(android.getAnnotationJar());
                    String classesOutputDirectory = androidConfiguration.getPublicRAndroidProject().getClassesOutput();
                    FileUtil.createDirectory(classesOutputDirectory);
                    int result = BuilderUtil.javac(classpathList, sourceList, classesOutputDirectory, androidConfiguration.isApkDebug());
                    if (result != 0) {
                        return false;
                    }
                    FileUtil.createDirectory((!androidConfiguration.isApkDebug() ? androidConfiguration.getPublicRAndroidProject().getOptimizedOriginalOutput() : androidConfiguration.getPublicAndroidProject().getOptimizedProguardOutput()));
                    String publicJar = (!androidConfiguration.isApkDebug() ? androidConfiguration.getPublicRAndroidProject().getOptimizedOriginalOutput() : androidConfiguration.getPublicAndroidProject().getOptimizedProguardOutput()) + "/" + PublicAndroidProject.PUBLIC + Constant.Symbol.DOT + Constant.File.JAR;
                    // BuilderUtil.executeJar(this.java.getJarExecutor(),
                    // publicJar, classesOutputDirectory);
                    BuilderUtil.jar(publicJar, classesOutputDirectory);
                }
                return true;
            }
        };
        this.dealWithCache(cacheOption);
        return true;
    }
}
