package com.oneliang.tools.builder.android.handler;

import java.util.Arrays;

import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.tools.builder.base.BuildException;

public class AutoDexHandler extends AbstractAndroidHandler {

	public boolean handle() {
		this.autoDex();
		return true;
	}

	protected void autoDex(){
		try{
			String allClassesJar=this.androidConfiguration.getMainAndroidProject().getAutoDexAllClassesJar();
			String androidManifestFullFilename=this.androidConfiguration.getPublicAndroidProject().getAndroidManifestOutput();
			String outputDirectory=this.androidConfiguration.getMainAndroidProject().getPrepareOutput();
			boolean debug=this.androidConfiguration.isApkDebug();
			AutoDexUtil.Option option=new AutoDexUtil.Option(Arrays.asList(allClassesJar), androidManifestFullFilename, outputDirectory, debug);
			option.minMainDex=true;
			option.attachBaseContext=true;
			option.mainDexOtherClassList=this.androidConfiguration.getAutoDexMainDexOtherClassList();
			AutoDexUtil.autoDex(option);
		}catch(Exception e){
			throw new BuildException(e);
		}
	}
}
