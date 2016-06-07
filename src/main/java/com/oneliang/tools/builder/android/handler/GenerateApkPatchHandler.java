package com.oneliang.tools.builder.android.handler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.common.Generator;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class GenerateApkPatchHandler extends AbstractAndroidHandler{

	public boolean handle() {
		final String inputAllClassesJarFullFilename=this.androidConfiguration.getApkPatchInputAllClassesJar();
		if(StringUtil.isNotBlank(inputAllClassesJarFullFilename)){
			final String patchOutput=this.androidConfiguration.getPatchAndroidProject().getOutputHome();
			final String patchPrepareOutput=this.androidConfiguration.getPatchAndroidProject().getPrepareOutput();
			FileUtil.createDirectory(patchOutput);
			FileUtil.createDirectory(patchPrepareOutput);
			String differentOutputFullFilename=this.androidConfiguration.getPatchAndroidProject().getDifferentOutput()+Constant.Symbol.SLASH_LEFT+AndroidProject.DIFFERENT_JAR;
			String thisTimeAllClassesJarFullFilename=this.androidConfiguration.getMainAndroidProject().getAutoDexAllClassesJar();
			FileUtil.differZip(differentOutputFullFilename, inputAllClassesJarFullFilename, thisTimeAllClassesJarFullFilename);
			String outputDexFullFilename=patchPrepareOutput+Constant.Symbol.SLASH_LEFT+AndroidProject.CLASSES_DEX;
			FileUtil.createFile(outputDexFullFilename);
			BuilderUtil.androidDx(outputDexFullFilename, Arrays.asList(differentOutputFullFilename), true);
			this.generateEmptyAndroidManifest(this.androidConfiguration.getPatchAndroidProject().getAndroidManifestOutput());
			//generate unsign apk
			String outputUnsignApkFullFilename=this.androidConfiguration.getPatchAndroidProject().getUnsignedApkFullFilename();
			BuilderUtil.generateApk(this.androidConfiguration.getPatchAndroidProject().getPrepareOutput(), outputUnsignApkFullFilename);
			//sign apk
			final String outputSignApkFullFilename=this.androidConfiguration.getPatchAndroidProject().getApkFullFilename();
			BuilderUtil.executeJarSigner(this.java.getJarSignerExecutor(), this.androidConfiguration.getJarKeystore(), this.androidConfiguration.getJarStorePassword(), this.androidConfiguration.getJarKeyPassword(), this.androidConfiguration.getJarKeyAlias(), outputSignApkFullFilename, outputUnsignApkFullFilename, this.androidConfiguration.getJarDigestalg(), this.androidConfiguration.getJarSigalg());
			String patchInfo=patchOutput+Constant.Symbol.SLASH_LEFT+"patch.info";
			FileUtil.createFile(patchInfo);
			OutputStream outputStream=null;
			try{
				outputStream=new FileOutputStream(patchInfo);
				outputStream.write(Generator.MD5File(outputSignApkFullFilename).getBytes(Constant.Encoding.UTF8));
				outputStream.flush();
			}catch (Exception e) {
				logger.error("write patch info failure", e);
			}finally{
				if(outputStream!=null){
					try {
						outputStream.close();
					} catch (IOException e) {
						logger.error("generate apk patch stream close failure.", e);
					}
				}
			}
		}
		return true;
	}
}
