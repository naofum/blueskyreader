package com.github.naofum.blueskyreader;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

public class AozoraBunkoIndexList extends AppCompatActivity {
    private static final int UPDATE_ID = 0;
	// private OnClickListener mButtonListener;
	private ArrayList<AozoraBunkoTopListInfo> mTopAuthorList = null;

	private ExpandableListView indexListView;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		if (Build.VERSION.SDK_INT >= 23) {
			if (ContextCompat.checkSelfPermission(this,
					android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED
					|| ContextCompat.checkSelfPermission(this,
					android.Manifest.permission.READ_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this,
						new String[]{
								android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
								android.Manifest.permission.READ_EXTERNAL_STORAGE},
						1);
			}
		}

    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.index_list);
		indexListView = (ExpandableListView)findViewById(R.id.indexListView);
		indexListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parentView, View childView,
									int groupPosition, int childPosition, long id) {
				return onChildClickAct((ExpandableListView) parentView, childView, groupPosition, childPosition, id);
			}
		});

		setTitle(getString(R.string.author_index));

    	fillTopAuthorList();

    	/*
    	// Create Button Listener for "Search" one.zz
    	Button button = (Button)findViewById(R.id.searchButton);
    	this.mButtonListener = new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO specification is needed.
				// Search text in "aozora.gr.jp"?;
			}
    	};
    	button.setOnClickListener(mButtonListener);
    	*/
    }

    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean retval;
		retval = super.onCreateOptionsMenu(menu);
		menu.add(0, UPDATE_ID, 0, R.string.menu_update);
		return retval;
	}

	
//	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean retval = false;
//		retval = super.onMenuItemSelected(featureId, item);
		
		switch (item.getItemId()) {
		case UPDATE_ID:
			// TODO update Authors database.
			updateAuthorsDb();
			break;
		}
		
		return retval;
	}


	private void updateAuthorsDb() {
		// TODO Auto-generated method stub
    	try {
    		// TODO
    		// if there expandable, updating only expandble. otherwise update all.
    	} catch (SQLException e) {
    		// TODO this case should be defined.
    	} 
	}


	private void fillTopAuthorList() {
		String TAG, header, searchURL;
		ArrayList<String> groups;
		AozoraBunkoTopListInfo info;
		this.mTopAuthorList = new ArrayList<AozoraBunkoTopListInfo>();
		
		// Create あ行
		TAG = "author A";
		header = getString(R.string.author_a);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_a.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_search_a), getString(R.string.author_search_i),
				getString(R.string.author_search_u), getString(R.string.author_search_e), getString(R.string.author_search_o)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);
		
		// Create か行
		TAG = "author KA";
		header = getString(R.string.author_ka);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_ka.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_search_ka), getString(R.string.author_search_ki),
				getString(R.string.author_search_ku), getString(R.string.author_search_ke), getString(R.string.author_search_ko)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);

		// Create さ行
		TAG = "author SA";
		header = getString(R.string.author_sa);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_sa.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_search_sa), getString(R.string.author_search_si),
				getString(R.string.author_search_su), getString(R.string.author_search_se), getString(R.string.author_search_so)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);
		
		// Create た行
		TAG = "author TA";
		header = getString(R.string.author_ta);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_ta.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_search_ta), getString(R.string.author_search_ti),
				getString(R.string.author_search_tu), getString(R.string.author_search_te), getString(R.string.author_search_to)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);

		// Create な行
		TAG = "author NA";
		header = getString(R.string.author_na);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_na.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_search_na), getString(R.string.author_search_ni),
				getString(R.string.author_search_nu), getString(R.string.author_search_ne), getString(R.string.author_search_no)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);

		// Create は行
		TAG = "author HA";
		header = getString(R.string.author_ha);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_ha.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_search_ha), getString(R.string.author_search_hi),
				getString(R.string.author_search_hu), getString(R.string.author_search_he), getString(R.string.author_search_ho)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);
		
		// Create ま行
		TAG = "author MA";
		header = getString(R.string.author_ma);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_ma.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_search_ma), getString(R.string.author_search_mi),
				getString(R.string.author_search_mu), getString(R.string.author_search_me), getString(R.string.author_search_mo)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);

		// Create や行
		TAG = "author YA";
		header = getString(R.string.author_ya);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_ya.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_search_ya),
				getString(R.string.author_search_yu), getString(R.string.author_search_yo)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);

		// Create ら行
		TAG = "author RA";
		header = getString(R.string.author_ra);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_ra.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_search_ra), getString(R.string.author_search_ri),
				getString(R.string.author_search_ru), getString(R.string.author_search_re), getString(R.string.author_search_ro)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);

		// Create わ行
		TAG = "author RA";
		header = getString(R.string.author_wa);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_wa.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_search_wa),
				getString(R.string.author_search_wo), getString(R.string.author_search_n)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);

		// Create その他
		TAG = "author others";
		header = getString(R.string.author_others);
		searchURL = "https://www.aozora.gr.jp/index_pages/person_zz.html";
		groups = new ArrayList<String>(Arrays.asList(new String[] {getString(R.string.author_others)}));
		info = new AozoraBunkoTopListInfo(TAG, header, searchURL, groups);
		this.mTopAuthorList.add(info);
		
		AozoraBunkoAuthorExpandableListAdapter mAdapter
		= new AozoraBunkoAuthorExpandableListAdapter(this, this.mTopAuthorList);
		
		indexListView.setAdapter(mAdapter);
	}

//	@Override
	public boolean onChildClickAct(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
//		boolean result = super.onChildClick(parent, v, groupPosition, childPosition, id);
		boolean result = false;
		Intent i = new Intent(this, AozoraBunkoAuthorList.class);
		AozoraBunkoTopListInfo info = this.mTopAuthorList.get(groupPosition);
		
		i.putExtra(AozoraBunkoAuthorList.KEY_PHONETICINDEX, (groupPosition * 5) + childPosition);
		i.putExtra(AozoraBunkoAuthorList.KEY_SEARCHURL, info.getSearchURLStr());
		i.putExtra(AozoraBunkoAuthorList.KEY_TITLE, info.getGroups().get(childPosition));
		
		startActivity(i);
		return result;
	}


}
