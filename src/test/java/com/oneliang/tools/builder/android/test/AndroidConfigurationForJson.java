package com.oneliang.tools.builder.android.test;

import com.oneliang.tools.builder.android.base.AndroidConfiguration;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.util.json.JsonUtil;

public class AndroidConfigurationForJson extends AndroidConfiguration{

	public void initializeAllProject() {
		String mainAndroidProjectJson="{name:'"+this.getProjectMain()+"',compileTarget:'android-23'}";
		AndroidProject mainAndroidProject=JsonUtil.jsonToObject(mainAndroidProjectJson, AndroidProject.class);
		mainAndroidProject.setWorkspace(this.projectWorkspace);
		mainAndroidProject.setOutputHome(this.buildOutput);
		mainAndroidProject.setDebug(this.apkDebug);
		mainAndroidProject.initialize();
		this.addProject(mainAndroidProject);
		super.initializeAllProject();
	}
}
