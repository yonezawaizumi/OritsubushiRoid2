package com.wsf_lp.android;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import android.content.res.AssetManager;
import android.content.res.Resources;

public class ZippedAssetStreamAndName {

	private static class Stream extends ZipInputStream {
		public Stream(InputStream stream) {
			super(stream);
		}
		public void close() throws IOException {
			closeEntry();
			super.close();
		}
	}

	private Stream input;
	private String fileName;
	private long size;

	public InputStream getInputStream() { return input; }
	public String getFileName() { return fileName; }
	public long getSize() { return size; }

	public static ZippedAssetStreamAndName open(Resources resources, String assetName) throws IOException, FileNotFoundException, ZipException {
		ZippedAssetStreamAndName result = new ZippedAssetStreamAndName();
		AssetManager assetManager = resources.getAssets();
		result.input = new Stream(assetManager.open(assetName, AssetManager.ACCESS_STREAMING));
		ZipEntry entry = result.input.getNextEntry();
		if(entry == null) {
			throw new FileNotFoundException();
		}
		result.fileName = entry.getName();
		result.size = entry.getSize();
		return result;
	}
}
