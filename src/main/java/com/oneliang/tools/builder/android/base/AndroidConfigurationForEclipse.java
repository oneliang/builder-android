package com.oneliang.tools.builder.android.base;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.oneliang.Constant;
import com.oneliang.tools.builder.base.BuildException;
import com.oneliang.tools.builder.base.Project;
import com.oneliang.util.common.JavaXmlUtil;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class AndroidConfigurationForEclipse extends AndroidConfiguration{

	protected static final String CLASSPATH=".classpath";
	protected static final String PROJECT_PROPERTIES="project.properties";

	protected void initializeAllProject() {
		initializeAllProjectFromEclipse();
		super.initializeAllProject();
	}

	private void initializeAllProjectFromEclipse() {
		AndroidProject mainAndroidProject=new AndroidProject(this.getProjectWorkspace(),this.getProjectMain());
		if(this.buildOutputEclipse){
			mainAndroidProject.setBuildType(AndroidProject.BUILD_TYPE_ECLIPSE);
			mainAndroidProject.setOutputHome(this.projectWorkspace);
		}else{
			mainAndroidProject.setBuildType(AndroidProject.BUILD_TYPE_DEFAULT);
			mainAndroidProject.setOutputHome(this.buildOutput);
		}
		mainAndroidProject.setDebug(this.apkDebug);
		mainAndroidProject.initialize();
		this.addProject(mainAndroidProject);
		//read the main project project.properties
		Queue<AndroidProject> queue=new ConcurrentLinkedQueue<AndroidProject>();
		queue.add(mainAndroidProject);
		while(!queue.isEmpty()){
			AndroidProject androidProject=queue.poll();
			String androidProjectName=androidProject.getName();
			if(this.androidProjectDexIdMap.containsKey(androidProjectName)){
				androidProject.setDexId(this.androidProjectDexIdMap.get(androidProjectName));
			}
			try{
				//read project.properties
				Properties properties=null;
				if(androidProject.equals(mainAndroidProject)){
					properties=FileUtil.getProperties(this.projectMainProperties);
				}else{
					properties=FileUtil.getProperties(androidProject.getHome()+Constant.Symbol.SLASH_LEFT+PROJECT_PROPERTIES);
				}
				Iterator<Entry<Object,Object>> iterator=properties.entrySet().iterator();
				List<String> dependProjectList=new ArrayList<String>();
				while(iterator.hasNext()){
					Entry<Object,Object> entry=iterator.next();
					String key=entry.getKey().toString();
					String value=entry.getValue().toString();
					if(key.startsWith("android.library.reference")){
						value=new File(value).getName();
						String dependProjectName=value;
						if(!this.projectMap.containsKey(dependProjectName)){
							AndroidProject parentAndroidProject=new AndroidProject(this.getProjectWorkspace(),dependProjectName);
							if(this.buildOutputEclipse){
								parentAndroidProject.setBuildType(AndroidProject.BUILD_TYPE_ECLIPSE);
								parentAndroidProject.setOutputHome(this.projectWorkspace);
							}else{
								parentAndroidProject.setBuildType(AndroidProject.BUILD_TYPE_DEFAULT);
								parentAndroidProject.setOutputHome(this.buildOutput);
							}
							parentAndroidProject.setDebug(this.apkDebug);
							parentAndroidProject.initialize();
							this.addProject(parentAndroidProject);
							queue.add(parentAndroidProject);
						}
						dependProjectList.add(dependProjectName);
					}else if(key.equals("target")){
						if(StringUtil.isNotBlank(value)){
							androidProject.setCompileTarget(value);
						}
					}else{
						this.parsingProjectProperties(androidProject, key, value);
					}
				}
				androidProject.setDependProjects(dependProjectList.toArray(new String[]{}));
				this.readProjectOtherProperties(androidProject);
				//read classpath
				String classpath=androidProject.getHome()+Constant.Symbol.SLASH_LEFT+CLASSPATH;
				List<String> sourceDirectoryList=new ArrayList<String>();
				Document document=null;
				try{
					document=JavaXmlUtil.parse(classpath);
				}catch (Exception e) {
					logger.warning("Use 'src' for source directory,because '"+classpath+"' file is not exist");
				}
				if(document!=null){
					Element root=document.getDocumentElement();
					NodeList entryElementList=root.getElementsByTagName("classpathentry");
					if(entryElementList!=null&&entryElementList.getLength()>0){
						for(int i=0;i<entryElementList.getLength();i++){
							NamedNodeMap namedNodeMap=entryElementList.item(i).getAttributes();
							Node kindNode=namedNodeMap.getNamedItem("kind");
							if(kindNode!=null){
								String value=kindNode.getNodeValue();
								if(value!=null&&value.equals("src")){
									Node pathNode=namedNodeMap.getNamedItem("path");
									if(pathNode!=null){
										String sourceDirectory=pathNode.getNodeValue();
										if(this.isSourceDirectory(androidProject, sourceDirectory)){
											sourceDirectoryList.add(sourceDirectory);
										}
									}
									
								}
							}
						}
					}
				}
				androidProject.setSources(sourceDirectoryList.toArray(new String[]{}));
			} catch (Exception e) {
				throw new BuildException(e);
			}
		}
	}

	/**
	 * is source directory when parsing classpath
	 * @param project
	 * @param sourceDirectory
	 * @return boolean
	 */
	private boolean isSourceDirectory(Project project, String sourceDirectory) {
		boolean result=false;
		if(sourceDirectory!=null&&(!sourceDirectory.equals("gen"))){
			result=true;
		}
		return result;
	}

	private void parsingProjectProperties(Project project, String key, String value) {
		if(project!=null&&project instanceof AndroidProject){
			AndroidProject androidProject=(AndroidProject)project;
			if(key.equals("only.compile.classpath")&&StringUtil.isNotBlank(value)){
				String[] onlyCompileClasspathArray=value.split(Constant.Symbol.COMMA);
				if(onlyCompileClasspathArray!=null){
					List<String> onlyCompileClasspathList=new ArrayList<String>();
					for(String onlyCompileClasspath:onlyCompileClasspathArray){
						if(FileUtil.isExist(onlyCompileClasspath)){
							File onlyCompileClasspathFile=new File(onlyCompileClasspath);
							onlyCompileClasspathList.add(onlyCompileClasspathFile.getAbsolutePath());
						}else{
							File onlyCompileClasspathFile=new File(androidProject.getHome(), onlyCompileClasspath);
							if(onlyCompileClasspathFile.exists()){
								onlyCompileClasspathList.add(onlyCompileClasspathFile.getAbsolutePath());
							}
						}
					}
					androidProject.setOnlyCompileClasspathList(onlyCompileClasspathList);
				}
			}
		}
	}

	private void readProjectOtherProperties(Project project) {
	}
}
