package com.oneliang.tools.builder.android.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.oneliang.tools.builder.android.base.AndroidConfiguration;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.util.common.DefaultClassProcessor;
import com.oneliang.util.json.JsonArray;
import com.oneliang.util.json.JsonUtil;

public class AndroidConfigurationForJson extends AndroidConfiguration{

	private String projectJsonFile=null;

	protected void initializeAllProject() {
		String json=readProjectJson();
		JsonArray jsonArray=new JsonArray(json);
		for(int i=0;i<jsonArray.length();i++){
			String androidProjectJson=jsonArray.getJsonObject(i).toString();
			AndroidProject androidProject=JsonUtil.jsonToObject(androidProjectJson, AndroidProject.class, new DefaultClassProcessor(){
				public Object changeClassProcess(Class<?> clazz, String[] values, String fieldName) {
					if(fieldName!=null&&fieldName.equals("onlyCompileClasspathList")){
						if(values!=null&&values.length>0){
							List<String> onlyCompileClasspathList=new ArrayList<String>();
							JsonArray onlyCompileJsonArray=new JsonArray(values[0]);
							for(int i=0;i<onlyCompileJsonArray.length();i++){
								onlyCompileClasspathList.add(onlyCompileJsonArray.getString(i));
							}
							return onlyCompileClasspathList;
						}else{
							return null;
						}
					}
					return super.changeClassProcess(clazz, values, fieldName);
				}
			});
			androidProject.setWorkspace(this.projectWorkspace);
			if(this.buildOutputEclipse){
				androidProject.setBuildType(AndroidProject.BUILD_TYPE_ECLIPSE);
				androidProject.setOutputHome(this.projectWorkspace);
			}else{
				androidProject.setBuildType(AndroidProject.BUILD_TYPE_DEFAULT);
				androidProject.setOutputHome(this.buildOutput);
			}
			androidProject.setDebug(this.apkDebug);
			androidProject.initialize();
			this.addProject(androidProject);
		}
		super.initializeAllProject();
	}

	private String readProjectJson(){
		StringBuilder jsonStringBuilder=new StringBuilder();
		BufferedReader bufferedReader=null;
		try{
			bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream(this.projectJsonFile)));
			String line=null;
			while((line=bufferedReader.readLine())!=null){
				jsonStringBuilder.append(line.trim());
			}
		}catch(Exception e){
			logger.error("Read project json exception.", e);
		}finally{
			if(bufferedReader!=null){
				try {
					bufferedReader.close();
				} catch (Exception e) {
					logger.error("BufferedReader close exception.", e);
				}
			}
		}
		return jsonStringBuilder.toString();
	}

	/**
	 * @param projectJsonFile the projectJsonFile to set
	 */
	public void setProjectJsonFile(String projectJsonFile) {
		this.projectJsonFile = projectJsonFile;
	}
}
