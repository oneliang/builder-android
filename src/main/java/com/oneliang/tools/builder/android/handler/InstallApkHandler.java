package com.oneliang.tools.builder.android.handler;

import com.oneliang.tools.builder.base.BuilderUtil;

public class InstallApkHandler extends AbstractAndroidHandler {

	public boolean handle() {
		String command=this.android.getAdbExecutor()+" install -r "+this.androidConfiguration.getMainAndroidProject().getZipAlignApkFullFilename();
		BuilderUtil.executeCommand(new String[]{command});
		command=this.android.getAdbExecutor()+" shell am start -n "+this.androidConfiguration.getMainAndroidProject().getPackageName()+"/"+this.androidConfiguration.getMainAndroidProjectMainActivityName();
		BuilderUtil.executeCommand(new String[]{command});
		return true;
	}
}
