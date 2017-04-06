package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.aapt.AaptUtil;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry.RType;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.android.base.AndroidProject.DirectoryType;
import com.oneliang.tools.builder.android.base.AndroidProjectForGradle;
import com.oneliang.tools.builder.android.base.PublicAndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.tools.builder.base.ChangedFile;
import com.oneliang.util.common.Generator;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class InitializeAndroidProjectForGradleHandler extends AbstractAndroidHandler {

    public static final String TEMPORARY_DATA_AAR_ANDROID_MANIFEST_XML_LIST = "aarAndroidManifestXmlList";
    private static final String AAR_AAPT = "aapt";
    private static final String AAR_AIDL = "aidl";
    private static final String AAR_ASSETS = "assets";
    private static final String AAR_JNI = "jni";
    private static final String AAR_LIBS = "libs";
    private static final String AAR_RES = "res";
    private static final String AAR_ANDROID_MANIFEST_XML = "AndroidManifest.xml";
    private static final String AAR_CLASSES_JAR = "classes.jar";
    private static final String GEN_R_CACHE = "gen_R_cache";

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
                        androidProject.addDependJar(gradleDependency);
                        if (androidProject.getCompileClasspathList() == null) {
                            androidProject.setCompileClasspathList(new ArrayList<String>());
                        }
                        List<String> compileClasspathList = androidProject.getCompileClasspathList();
                        compileClasspathList.add(gradleDependency);
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
                String aarResOutput = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AAR_RES;
                androidProject.getResourceDirectoryList().add(0, aarResOutput);

                // aar libs
                String aarLibsOutput = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AAR_LIBS;
                String aarJniOutput = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AAR_JNI;
                androidProject.getLibsDirectoryList().add(aarLibsOutput);
                androidProject.getLibsDirectoryList().add(aarJniOutput);

                // aar assets
                String aarAssetsOutput = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AAR_ASSETS;
                androidProject.getAssetsDirectoryList().add(aarAssetsOutput);

                // aar classes.jar
                String aarClassesJar = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AAR_CLASSES_JAR;
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
                String aarAndroidManifest = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AAR_ANDROID_MANIFEST_XML;
                androidManifestXmlList.add(aarAndroidManifest);

                // aar aidl
                String aarAidlOutput = aarFileUnzipOutput + Constant.Symbol.SLASH_LEFT + AAR_AIDL;
                if (FileUtil.hasFile(aarAidlOutput)) {
                    androidProject.getSourceDirectoryList().add(aarAidlOutput);
                }
            }
        }
        androidManifestXmlList = this.filterDuplicateFile(androidManifestXmlList);
        this.androidConfiguration.putTemporaryData(TEMPORARY_DATA_AAR_ANDROID_MANIFEST_XML_LIST, androidManifestXmlList);

        generatePublicRDotTxt();

        generateAarRDotJava(aarProjectList);
        return true;
    }

    private void generateAarRDotJava(List<AarProject> aarProjectList) {
        if (aarProjectList == null || aarProjectList.isEmpty()) {
            return;
        }
        String rTxtFullFilename = this.androidConfiguration.getApkPatchInputRTxt();
        Map<RType, Set<RDotTxtEntry>> rTypeResourceMap = null;
        if (StringUtil.isNotBlank(rTxtFullFilename) && FileUtil.isExist(rTxtFullFilename)) {
            rTypeResourceMap = AaptUtil.readRTxt(rTxtFullFilename);
        } else {
            rTypeResourceMap = new HashMap<RType, Set<RDotTxtEntry>>();
        }
        final Map<RType, Set<RDotTxtEntry>> finalRTypeResourceMap = rTypeResourceMap;
        for (AarProject aarProject : aarProjectList) {
            final String aarResOutput = aarProject.unzipOutput + Constant.Symbol.SLASH_LEFT + AAR_RES;
            final String aarAndroidManifest = aarProject.unzipOutput + Constant.Symbol.SLASH_LEFT + AAR_ANDROID_MANIFEST_XML;
            if (FileUtil.hasFile(aarResOutput)) {
                String aarResCacheFullFilename = aarProject.unzipOutput + Constant.Symbol.SLASH_LEFT + CACHE_RESOURCE_FILE;
                CacheOption cacheOption = new CacheOption(aarResCacheFullFilename, Arrays.asList(aarResOutput));
                cacheOption.changedFileProcessor = new CacheOption.ChangedFileProcessor() {
                    public boolean process(Iterable<ChangedFile> changedFileIterable) {
                        if (changedFileIterable != null && changedFileIterable.iterator().hasNext()) {
                            Map<RType, Set<RDotTxtEntry>> allRTypeResourceMap = AaptUtil.collectResource(Arrays.asList(aarResOutput), finalRTypeResourceMap).getRTypeResourceMap();
                            String packageName = AndroidProject.parsePackageName(aarAndroidManifest);
                            String outputDirectory = androidConfiguration.getPublicAndroidProject().getGenOutput();
                            AaptUtil.writeRJava(outputDirectory, packageName, allRTypeResourceMap, false);
                            return true;
                        } else {
                            return false;
                        }
                    }
                };
                this.dealWithCache(cacheOption);
            }
        }
    }

    private void generatePublicRDotTxt() {
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
                    if (!androidConfiguration.getMainAndroidProject().getAndroidManifestList().isEmpty()) {
                        int result = BuilderUtil.executeAndroidAaptToGenerateR(android.getAaptExecutor(), androidConfiguration.getMainAndroidProject().getAndroidManifestList().get(0), resourceDirectoryList, publicOutput, Arrays.asList(androidConfiguration.getMainAndroidApiJar()), androidConfiguration.isApkDebug());
                        if (result == 0) {
                            saveCache = true;
                        }
                    }
                }
                return saveCache;
            }
        };
        this.dealWithCache(cacheOption);
        
        if (FileUtil.isExist(publicRDotTxt)) {
            androidConfiguration.setApkPatchInputRTxt(publicRDotTxt);
        }
    }

    private class AarProject {
        public final String name;
        public final String unzipOutput;

        public AarProject(String name, String unzipOutput) {
            this.name = name;
            this.unzipOutput = unzipOutput;
        }
    }
}
