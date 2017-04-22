package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.android.base.AndroidProjectForGradle;
import com.oneliang.util.common.Generator;
import com.oneliang.util.file.FileUtil;

public class InitializeAndroidProjectForGradleHandler extends AbstractAndroidHandler {

    public static final String TEMPORARY_DATA_AAR_ANDROID_MANIFEST_XML_LIST = "aarAndroidManifestXmlList";
    public static final String TEMPORARY_DATA_AAR_PROJECT_LIST = "aarProjectList";

    public boolean handle() {
        Map<String, String> aarFileCacheMap = new HashMap<String, String>();
        List<AndroidProject> androidProjectList = this.androidConfiguration.getAndroidProjectList();
        List<String> androidManifestXmlList = new ArrayList<String>();
        List<AarProject> aarProjectList = new ArrayList<AarProject>();
        for (AndroidProject androidProject : androidProjectList) {
            if (!(androidProject instanceof AndroidProjectForGradle)) {
                continue;
            }
            AndroidProjectForGradle androidProjectForGradle = (AndroidProjectForGradle) androidProject;
            List<String> gradleDependencyList = androidProjectForGradle.getGradleDependencyList();
            if (gradleDependencyList == null || gradleDependencyList.isEmpty()) {
                continue;
            }
            for (String gradleDependency : gradleDependencyList) {
                File dependencyFile = new File(gradleDependency);
                String dependencyFilename = dependencyFile.getName();
                if (!dependencyFilename.endsWith(".aar")) {
                    if (dependencyFilename.endsWith(Constant.Symbol.DOT + Constant.File.JAR)) {
                        String dependencyFullFilename = dependencyFile.getAbsolutePath();
                        if (!FileUtil.isExist(dependencyFullFilename)) {
                            continue;
                        }
                        androidProject.addDependJar(dependencyFullFilename);
                        if (androidProject.getCompileClasspathList() == null) {
                            androidProject.setCompileClasspathList(new ArrayList<String>());
                        }
                        List<String> compileClasspathList = androidProject.getCompileClasspathList();
                        compileClasspathList.add(dependencyFullFilename);
                    }
                    continue;
                }
                final String aarBuildCacheOutput = this.androidConfiguration.getBuildOutput() + Constant.Symbol.SLASH_LEFT + "aar_build_cache";
                FileUtil.createDirectory(aarBuildCacheOutput);
                String aarFileUnzipOutput = null;
                String aarFileFullFilename = dependencyFile.getAbsolutePath();
                if (aarFileCacheMap.containsKey(aarFileFullFilename)) {
                    aarFileUnzipOutput = aarFileCacheMap.get(aarFileFullFilename);
                } else {
                    aarFileUnzipOutput = aarBuildCacheOutput + Constant.Symbol.SLASH_LEFT + dependencyFilename;
                    aarFileCacheMap.put(aarFileFullFilename, aarFileUnzipOutput);

                    aarProjectList.add(new AarProject(dependencyFilename, aarFileUnzipOutput));

                    String aarMd5FullFilename = aarBuildCacheOutput + Constant.Symbol.SLASH_LEFT + dependencyFilename + "_md5.txt";
                    String newAarMd5 = Generator.MD5File(gradleDependency);
                    boolean aarHasChanged = false;
                    if (FileUtil.isExist(aarMd5FullFilename)) {
                        try {
                            String oldAarMd5 = new String(FileUtil.readFile(aarMd5FullFilename), Constant.Encoding.UTF8);
                            if (!oldAarMd5.equals(newAarMd5)) {
                                aarHasChanged = true;
                            }
                        } catch (Exception e) {
                            logger.error(Constant.Base.EXCEPTION, e);
                        }
                    } else {
                        aarHasChanged = true;
                    }
                    if (aarHasChanged) {
                        try {
                            FileUtil.writeFile(aarMd5FullFilename, newAarMd5.getBytes(Constant.Encoding.UTF8));
                        } catch (Exception e) {
                            logger.error(Constant.Base.EXCEPTION, e);
                        }
                        FileUtil.unzip(gradleDependency, aarFileUnzipOutput);
                    }
                }
                // aar res
                String aarResOutput = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AarProject.AAR_RES;
                androidProject.getResourceDirectoryList().add(0, aarResOutput);

                // aar libs
                String aarLibsOutput = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AarProject.AAR_LIBS;
                String aarJniOutput = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AarProject.AAR_JNI;
                androidProject.getLibsDirectoryList().add(aarLibsOutput);
                androidProject.getLibsDirectoryList().add(aarJniOutput);

                // aar assets
                String aarAssetsOutput = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AarProject.AAR_ASSETS;
                androidProject.getAssetsDirectoryList().add(aarAssetsOutput);

                // aar classes.jar
                String aarClassesJar = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AarProject.AAR_CLASSES_JAR;
                if (androidProject.getCompileClasspathList() == null) {
                    androidProject.setCompileClasspathList(new ArrayList<String>());
                }
                androidProject.addDependJar(aarClassesJar);
                List<String> compileClasspathList = androidProject.getCompileClasspathList();
                compileClasspathList.add(aarClassesJar);
                FileUtil.MatchOption matchOption = new FileUtil.MatchOption(aarLibsOutput);
                matchOption.fileSuffix = Constant.Symbol.DOT + Constant.File.JAR;
                List<String> libsJarList = FileUtil.findMatchFile(matchOption);
                if (libsJarList != null && !libsJarList.isEmpty()) {
                    for (String libsJar : libsJarList) {
                        androidProject.addDependJar(libsJar);
                        compileClasspathList.add(libsJar);
                    }
                }

                // aar AndroidManifest.xml
                String aarAndroidManifest = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AarProject.AAR_ANDROID_MANIFEST_XML;
                androidManifestXmlList.add(aarAndroidManifest);

                // aar aidl
                String aarAidlOutput = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AarProject.AAR_AIDL;
                if (FileUtil.hasFile(aarAidlOutput)) {
                    androidProject.getSourceDirectoryList().add(aarAidlOutput);
                }
            }
        }
        androidManifestXmlList = this.filterDuplicateFile(androidManifestXmlList);
        this.androidConfiguration.putTemporaryData(TEMPORARY_DATA_AAR_ANDROID_MANIFEST_XML_LIST, androidManifestXmlList);
        this.androidConfiguration.putTemporaryData(TEMPORARY_DATA_AAR_PROJECT_LIST, aarProjectList);
        return true;
    }

    public static class AarProject {
        public static final String AAR_AAPT = "aapt";
        public static final String AAR_AIDL = "aidl";
        public static final String AAR_ASSETS = "assets";
        public static final String AAR_JNI = "jni";
        public static final String AAR_LIBS = "libs";
        public static final String AAR_RES = "res";
        public static final String AAR_ANDROID_MANIFEST_XML = "AndroidManifest.xml";
        public static final String AAR_CLASSES_JAR = "classes.jar";
        public final String name;
        public final String unzipOutput;

        public AarProject(String name, String unzipOutput) {
            this.name = name;
            this.unzipOutput = unzipOutput;
        }
    }
}
