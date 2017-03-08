package com.oneliang.tools.builder.android.handler;

import com.oneliang.tools.builder.base.BuilderUtil;

public class SignApkHandler extends AbstractAndroidHandler {

	public boolean handle() {
		if(!this.androidConfiguration.isAllAssetsFileHasCache()||!isAllCompileFileHasCache(this.androidConfiguration.getAndroidProjectList())){
			BuilderUtil.executeJarSigner(java.getJarSignerExecutor(), this.androidConfiguration.getJarKeystore(), this.androidConfiguration.getJarStorePassword(), this.androidConfiguration.getJarKeyPassword(), this.androidConfiguration.getJarKeyAlias(), this.androidConfiguration.getMainAndroidProject().getApkFullFilename(), this.androidConfiguration.getMainAndroidProject().getUnsignedApkFullFilename(), this.androidConfiguration.getJarDigestalg(), this.androidConfiguration.getJarSigalg());
		}
		return true;
	}
}
