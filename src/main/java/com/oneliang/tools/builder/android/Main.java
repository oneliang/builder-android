package com.oneliang.tools.builder.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.oneliang.util.common.StringUtil;
import com.oneliang.util.logging.AbstractLogger;
import com.oneliang.util.logging.BaseLogger;
import com.oneliang.util.logging.ComplexLogger;
import com.oneliang.util.logging.FileLogger;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public class Main {

	public static void main(String[] args) {
		args=new String[]{"bin/config/test-android-builder.xml","target.task=clean"};
		args=new String[]{"bin/config/test-android-builder.xml","target.task=zipAlignApk"};
		//first initialize the class
		try {
			Class.forName(com.oneliang.tools.builder.Main.class.getName(), true, Thread.currentThread().getContextClassLoader());
		} catch (ClassNotFoundException e) {
		}
		//then override the logger configuration
		String projectRealPath=new File(StringUtil.BLANK).getAbsolutePath();
		List<AbstractLogger> loggerList=new ArrayList<AbstractLogger>();
		loggerList.add(new BaseLogger(Logger.Level.VERBOSE));
		loggerList.add(new FileLogger(Logger.Level.VERBOSE,new File(projectRealPath+"/log/default.log")));
		Logger logger=new ComplexLogger(Logger.Level.DEBUG, loggerList);
		LoggerManager.registerLogger("*", logger);
		//third execute main
		com.oneliang.tools.builder.Main.main(args);
	}
}
