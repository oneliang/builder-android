package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.base.AndroidProject.DirectoryType;
import com.oneliang.tools.builder.base.ChangedFile;
import com.oneliang.util.file.FileUtil;

public class CopySoFileHandler extends AbstractAndroidHandler {

	public boolean handle() {
		String soFileCacheFullFilename=this.androidConfiguration.getPublicRAndroidProject().getCacheOutput()+Constant.Symbol.SLASH_LEFT+CACHE_SO_FILE;
		final List<String> libDirectoryList=this.androidConfiguration.findDirectoryOfAndroidProjectList(this.androidConfiguration.getAndroidProjectList(),DirectoryType.LIBS);
		CacheOption cacheOption=new CacheOption(soFileCacheFullFilename, libDirectoryList);
		cacheOption.fileSuffix=Constant.Symbol.DOT+Constant.File.SO;
		cacheOption.changedFileProcessor=new ChangedFileProcessor() {
			public boolean process(Iterable<ChangedFile> changedFileIterable) {
				if(changedFileIterable!=null&&changedFileIterable.iterator().hasNext()){
					String prepareLibOutput=androidConfiguration.getMainAndroidProject().getPrepareLibOutput();
					FileUtil.createDirectory(prepareLibOutput);
					Iterator<ChangedFile> changedFileIterator=changedFileIterable.iterator();
					while(changedFileIterator.hasNext()){
						ChangedFile changedFile=changedFileIterator.next();
						String directory=changedFile.directory;
						String fullFilename=changedFile.fullFilename;
						String from=fullFilename;
						String relativePath=new File(fullFilename).getAbsolutePath().substring(new File(directory).getAbsolutePath().length()+1);
						String to=prepareLibOutput+Constant.Symbol.SLASH_LEFT+relativePath;
						FileUtil.copyFile(from, to, FileUtil.FileCopyType.FILE_TO_FILE);
					}
				}
				return true;
			}
		};
		this.dealWithCache(cacheOption);
		return true;
	}
}
