package com.oneliang.tools.builder.android.handler;

import com.oneliang.Constant;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.file.FileUtil;

public class AfterMergeDexHandler extends MultiAndroidProjectDexHandler {

	public boolean handle() {
		String outputDirectory=this.androidConfiguration.getMainAndroidProject().getPrepareAssetsOutput()+"/dex";
		FileUtil.createDirectory(outputDirectory);
		if(dexId!=0){
			String dexOutputDirectory=this.androidConfiguration.getMainAndroidProject().getDexOutput()+"/"+dexId;
			String unsignSecondDexOutputJar=this.androidConfiguration.getMainAndroidProject().getDexOutput()+"/unsign-dex"+dexId+Constant.Symbol.DOT+Constant.File.APK;
			String signSecondDexOutputJar=this.androidConfiguration.getMainAndroidProject().getDexOutput()+"/dex"+dexId+Constant.Symbol.DOT+Constant.File.APK;
//			this.generateOtherDexEmptyAndroidManifest(dexId, dexOutputDirectory);
			//jar
			BuilderUtil.executeJar(this.java.getJarExecutor(), unsignSecondDexOutputJar, dexOutputDirectory,false);
			//jar signer
			BuilderUtil.executeJarSigner(java.getJarSignerExecutor(), this.androidConfiguration.getJarKeystore(), this.androidConfiguration.getJarStorePassword(), this.androidConfiguration.getJarKeyPassword(), this.androidConfiguration.getJarKeyAlias(), signSecondDexOutputJar, unsignSecondDexOutputJar, this.androidConfiguration.getJarDigestalg(), this.androidConfiguration.getJarSigalg());
			//split jar
		}
		return true;
	}
}
