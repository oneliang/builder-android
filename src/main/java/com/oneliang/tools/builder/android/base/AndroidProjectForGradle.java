package com.oneliang.tools.builder.android.base;

import java.util.List;

import com.oneliang.tools.builder.android.base.AndroidProject;

public class AndroidProjectForGradle extends AndroidProject {

    private List<String> gradleDependencyList = null;

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
}
