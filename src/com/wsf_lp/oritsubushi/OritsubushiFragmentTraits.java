package com.wsf_lp.oritsubushi;

public interface OritsubushiFragmentTraits {
	//現在居る階層を返す 0 がトップ階層
	public int getCurrentDepth();
	//backキーを処理する 処理したらtrueを、処理しなかったらfalseを返す
	public boolean onBackPressed();
}
