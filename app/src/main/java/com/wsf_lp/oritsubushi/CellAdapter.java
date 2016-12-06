package com.wsf_lp.oritsubushi;

import java.util.List;

import com.wsf_lp.mapapp.data.CellItem;
import com.wsf_lp.oritsubushi.R;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CellAdapter extends BaseAdapter {
	protected final List<? extends CellItem> cells;
	protected final LayoutInflater inflater;
	protected final Activity activity;

	public CellAdapter(List<? extends CellItem> cells, Activity activity) {
		this.cells = cells;
		this.inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.activity = activity;
	}
	
	@Override
	public int getCount() {
		return cells.size();
	}

	@Override
	public Object getItem(int position) {
		return cells.get(position);
	}

	@Override
	public long getItemId(int position) {
		return cells.get(position).getCellId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if(view == null) {
			view = inflater.inflate(R.layout.cell, null);
		}
		CellItem cell = (CellItem)getItem(position);
		if(cell != null) {
			final Resources resources = activity.getResources();
			((ImageView)view.findViewById(R.id.status_icon)).setImageDrawable(cell.getStatusIcon(resources));
			((TextView)view.findViewById(R.id.title)).setText(cell.getTitle(resources));
			((TextView)view.findViewById(R.id.description)).setText(cell.getDescription(resources));
		}
		return view;
	}

}
