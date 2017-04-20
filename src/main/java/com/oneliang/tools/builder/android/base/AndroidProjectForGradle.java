package com.oneliang.tools.builder.android.base;

import java.util.List;

import com.oneliang.tools.builder.android.base.AndroidProject;

public class AndroidProjectForGradle extends AndroidProject {

    private List<String> gradleDependencyList = null;
    private List<BuildConfig> buildConfigList = null;

    public AndroidProjectForGradle(String workspace, String name, String outputHome) {
        super(workspace, name, outputHome, BUILD_TYPE_DEFAULT);
    }

    /**
     * @return the gradleDependencyList
     */
    public List<String> getGradleDependencyList() {
        return gradleDependencyList;
    }

    /**
     * @param gradleDependencyList
     *            the gradleDependencyList to set
     */
    public void setGradleDependencyList(List<String> gradleDependencyList) {
        this.gradleDependencyList = gradleDependencyList;
    }

    /**
     * @return the buildConfigList
     */
    public List<BuildConfig> getBuildConfigList() {
        return buildConfigList;
    }

    /**
     * @param buildConfigList
     *            the buildConfigList to set
     */
    public void setBuildConfigList(List<BuildConfig> buildConfigList) {
        this.buildConfigList = buildConfigList;
    }

    public static class BuildConfig {
        public final String type;
        public final String name;
        public final String value;

        public BuildConfig(String type, String name, String value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }

        public String toString() {
            return "public static final " + this.type + " " + this.name + " = " + this.value + ";";
        }
    }
}
