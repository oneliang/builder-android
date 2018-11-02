package com.oneliang.tools.builder.android.base;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.oneliang.Constants;
import com.oneliang.tools.builder.android.base.AndroidProjectForGradle.BuildConfig;
import com.oneliang.tools.builder.base.KeyValue;
import com.oneliang.tools.builder.base.Project;
import com.oneliang.tools.builder.java.base.Java;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.json.JsonArray;
import com.oneliang.util.json.JsonObject;

public class AndroidConfigurationForJson extends AndroidConfiguration {

    public static final String TEMPORARY_DATA_MANIFEST_REPLACE_KEYWORD_LIST = "manifestReplaceKeywordList";
    private String projectJsonFile = null;

    private JsonObject jsonObject = null;
    private String compileTarget = null;

    protected void initialize() {
        String json = FileUtil.readFileContentIgnoreLine(new File(this.projectJsonFile).getAbsolutePath());
        this.jsonObject = new JsonObject(json);
        this.setJarKeyAlias(this.jsonObject.getString("keyAlias"));
        this.setJarKeyPassword(this.jsonObject.getString("keyPassword"));
        this.setJarKeystore(this.jsonObject.getString("storeFile"));
        this.setJarStorePassword(this.jsonObject.getString("storePassword"));
        if (StringUtil.isBlank(this.getBuildOutput())) {
            this.setBuildOutput(new File(this.jsonObject.getString("buildDirectory"), "builder-gen").getAbsolutePath());
        }
        this.setProjectMain(this.jsonObject.getString("projectMain"));
        this.setProjectWorkspace(this.jsonObject.getString("projectWorkspace"));
        this.setPackageName(this.jsonObject.getString("packageName"));
        this.setMinSdkVersion(16);
        this.setTargetSdkVersion(23);
        super.initialize();
    }

    /**
     * initialize all project from json
     */
    private void initializeAllProjectFromJson() {
        String javaSdk = jsonObject.getString("javaSdk");
        this.java = new Java(javaSdk);
        String androidSdk = jsonObject.getString("androidSdk");
        String androidBuildToolsVersion = jsonObject.getString("buildToolsVersion");
        this.android = new Android(androidSdk, androidBuildToolsVersion);
        this.compileTarget = jsonObject.getString("compileTarget");
        // all projects
        this.parseAllProject();
        // manifest
        this.parseManifest();
        Iterator<Entry<String, Project>> iterator = projectMap.entrySet().iterator();
        while (iterator.hasNext()) {
            logger.info(iterator.next().getValue());
        }
    }

    protected void initializeAllProject() {
        this.initializeAllProjectFromJson();
        super.initializeAllProject();
    }

    /**
     * parse all project
     */
    private void parseAllProject() {
        JsonArray projectsJsonArray = jsonObject.getJsonArray("projects");
        int projectsLength = projectsJsonArray.length();
        for (int index = 0; index < projectsLength; index++) {
            JsonObject projectJsonObject = projectsJsonArray.getJsonObject(index);
            String projectName = projectJsonObject.getString("name");
            projectName = projectName.replace(Constants.Symbol.SLASH_RIGHT, Constants.Symbol.SLASH_LEFT);
            String path = projectJsonObject.getString("path");
            String pathFullFilename = new File(path).getAbsolutePath().replace(Constants.Symbol.SLASH_RIGHT, Constants.Symbol.SLASH_LEFT);
            pathFullFilename = pathFullFilename.substring(0, pathFullFilename.length() - projectName.length() - 1);
            AndroidProjectForGradle androidProject = new AndroidProjectForGradle(pathFullFilename, projectName, this.buildOutput);
            androidProject.setDebug(this.apkDebug);
            androidProject.initialize();
            if (projectJsonObject.has("src")) {
                JsonArray srcJsonArray = projectJsonObject.getJsonArray("src");
                int length = srcJsonArray.length();
                for (int i = 0; i < length; i++) {
                    androidProject.getSourceDirectoryList().add(new File(srcJsonArray.getString(i)).getAbsolutePath());
                }
            }
            if (projectJsonObject.has("res")) {
                JsonArray resJsonArray = projectJsonObject.getJsonArray("res");
                int length = resJsonArray.length();
                for (int i = 0; i < length; i++) {
                    String resourceDirectory = resJsonArray.getString(i);
                    if (!FileUtil.isExist(resourceDirectory)) {
                        continue;
                    }
                    androidProject.getResourceDirectoryList().add(resourceDirectory);
                }
            }
            // TODO: fix toolkit-box bug
            String res = androidProject.getHome() + Constants.Symbol.SLASH_LEFT + "res";
            if (FileUtil.isExist(res)) {
                androidProject.getResourceDirectoryList().add(res);
            }
            if (projectJsonObject.has("assets")) {
                JsonArray assetsJsonArray = projectJsonObject.getJsonArray("assets");
                int length = assetsJsonArray.length();
                for (int i = 0; i < length; i++) {
                    androidProject.getAssetsDirectoryList().add(assetsJsonArray.getString(i));
                }
            }
            if (projectJsonObject.has("jni")) {
                JsonArray jniJsonArray = projectJsonObject.getJsonArray("jni");
                int length = jniJsonArray.length();
                for (int i = 0; i < length; i++) {
                    androidProject.getLibsDirectoryList().add(jniJsonArray.getString(i));
                }
            }
            if (projectJsonObject.has("jniLibs")) {
                JsonArray jniLibsJsonArray = projectJsonObject.getJsonArray("jniLibs");
                int length = jniLibsJsonArray.length();
                for (int i = 0; i < length; i++) {
                    String directory = jniLibsJsonArray.getString(i);
                    if (!androidProject.getLibsDirectoryList().contains(directory)) {
                        androidProject.getLibsDirectoryList().add(directory);
                    }
                }
            }
            // TODO: fix toolkit-box bug
            String libs = androidProject.getHome() + Constants.Symbol.SLASH_LEFT + "libs";
            if (FileUtil.isExist(libs)) {
                androidProject.getLibsDirectoryList().add(libs);
            }
            if (projectJsonObject.has("dependencies")) {
                JsonArray dependenciesJsonArray = projectJsonObject.getJsonArray("dependencies");
                int dependenciesLength = dependenciesJsonArray.length();
                String[] dependProjects = new String[dependenciesLength];
                for (int i = 0; i < dependenciesLength; i++) {
                    dependProjects[i] = dependenciesJsonArray.getString(i);
                }
                androidProject.setDependProjects(dependProjects);
            }

            if (projectJsonObject.has("classpath")) {
                JsonArray classpathJsonArray = projectJsonObject.getJsonArray("classpath");
                int length = classpathJsonArray.length();
                List<String> gradleDependencyList = new ArrayList<String>();
                for (int i = 0; i < length; i++) {
                    String classpathFile = classpathJsonArray.getString(i);
                    gradleDependencyList.add(classpathFile);
                }
                androidProject.setGradleDependencyList(gradleDependencyList);
            }
            // must after androidProject.initialize()
            // androidProject.getAndroidManifestList().clear();
            if (projectJsonObject.has("manifest")) {
                androidProject.getAndroidManifestList().add(projectJsonObject.getString("manifest"));
                androidProject.parsePackageName();
            }
            if (projectJsonObject.has("apt")) {
                JsonArray aptJsonArray = projectJsonObject.getJsonArray("apt");
                int length = aptJsonArray.length();
                List<String> compileProcessorPathList = new ArrayList<String>();
                for (int i = 0; i < length; i++) {
                    String compileProcessorPath = aptJsonArray.getString(i);
                    compileProcessorPathList.add(compileProcessorPath);
                }
                androidProject.setCompileProcessorPathList(compileProcessorPathList);
            }

            if (projectJsonObject.has("onlyCompile")) {
                JsonArray onlyCompileJsonArray = projectJsonObject.getJsonArray("onlyCompile");
                int length = onlyCompileJsonArray.length();
                List<String> onlyCompileClasspathList = new ArrayList<String>();
                for (int i = 0; i < length; i++) {
                    String onlyCompile = onlyCompileJsonArray.getString(i);
                    onlyCompileClasspathList.add(new File(onlyCompile).getAbsolutePath());
                }
                androidProject.setOnlyCompileClasspathList(onlyCompileClasspathList);
            }

            if (projectJsonObject.has("provided")) {
                boolean provided = projectJsonObject.getBoolean("provided");
                androidProject.setProvided(provided);
            }

            if (projectJsonObject.has("buildConfig")) {
                JsonArray buildConfigJsonArray = projectJsonObject.getJsonArray("buildConfig");
                int length = buildConfigJsonArray.length();
                List<BuildConfig> buildConfigList = new ArrayList<BuildConfig>();
                for (int i = 0; i < length; i++) {
                    JsonObject buildConfigJsonObject = buildConfigJsonArray.getJsonObject(i);
                    String type = buildConfigJsonObject.getString("type");
                    String name = buildConfigJsonObject.getString("name");
                    String value = buildConfigJsonObject.getString("value");
                    BuildConfig buildConfig = new BuildConfig(type, name, value);
                    buildConfigList.add(buildConfig);
                }
                androidProject.setBuildConfigList(buildConfigList);
            }

            androidProject.setCompileTarget(this.compileTarget);
            for (String libsDirectory : androidProject.getLibsDirectoryList()) {
                FileUtil.MatchOption matchOption = new FileUtil.MatchOption(libsDirectory);
                matchOption.fileSuffix = Constants.Symbol.DOT + Constants.File.JAR;
                androidProject.getDependJarSet().addAll(FileUtil.findMatchFile(matchOption));
            }

            this.addProject(androidProject);
        }
    }

    /**
     * parse manifest
     */
    private void parseManifest() {
        if (this.jsonObject.has("manifest")) {
            JsonObject manifestJsonObject = this.jsonObject.getJsonObject("manifest");
            if (manifestJsonObject.has("replaceKeyword")) {
                List<KeyValue<String, String>> keyValueList = new ArrayList<KeyValue<String, String>>();
                JsonArray replaceKeywordJsonArray = manifestJsonObject.getJsonArray("replaceKeyword");
                int length = replaceKeywordJsonArray.length();
                for (int i = 0; i < length; i++) {
                    JsonObject replaceKeywordJsonObject = replaceKeywordJsonArray.getJsonObject(i);
                    if (!replaceKeywordJsonObject.has("key") || !replaceKeywordJsonObject.has("value")) {
                        continue;
                    }
                    String key = replaceKeywordJsonObject.getString("key");
                    String value = replaceKeywordJsonObject.getString("value");
                    KeyValue<String, String> keyValue = new KeyValue<String, String>(key, value);
                    keyValueList.add(keyValue);
                }
                temporaryDataMap.put(TEMPORARY_DATA_MANIFEST_REPLACE_KEYWORD_LIST, keyValueList);
            }
            if (manifestJsonObject.has("metadata")) {

            }
        }
    }

    /**
     * @param projectJsonFile
     *            the projectJsonFile to set
     */
    public void setProjectJsonFile(String projectJsonFile) {
        this.projectJsonFile = projectJsonFile;
    }
}
