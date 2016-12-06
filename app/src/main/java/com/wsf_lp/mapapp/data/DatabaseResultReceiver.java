package com.wsf_lp.mapapp.data;

public interface DatabaseResultReceiver {
	public void onDatabaseResult(long sequence, String methodName, Object result);
}
