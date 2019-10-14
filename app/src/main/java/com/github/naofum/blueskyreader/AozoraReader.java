package com.github.naofum.blueskyreader;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.v7.app.AppCompatActivity;

public class AozoraReader extends AppCompatActivity {

	private AozoraReaderBookmarksDbAdapter mDbAdapter;
	private int bookmarkSize;
    private static final int INDEX_ID = 0;
	private static final int ActivityIndex = 0;

	private ListView mainListView;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	this.mDbAdapter = new AozoraReaderBookmarksDbAdapter(this);
		this.mDbAdapter.open();
		Cursor c = this.mDbAdapter.fetchAllBookmarks();
		this.bookmarkSize = c.getCount();
		c.close();
		
		if (this.bookmarkSize == 0) {
			// No bookmarks so go to Index List.
			startIndexListActivity();
		} else {
			// Display bookmarks as List.
			setContentView(R.layout.main);
			mainListView = (ListView)findViewById(R.id.mainListView);
			mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				public void onItemClick(AdapterView parentView, View childView,
										int position, long id) {
					onListItemClick(mainListView, childView, position, id);
				}
			});
			fillBookmarks();
		}
    }

	private void fillBookmarks() {
		Cursor c = this.mDbAdapter.fetchAllBookmarks();
		this.bookmarkSize = c.getCount();

		ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, R.layout.main, R.id.main_row);
		c.moveToLast();
		if (c.getColumnCount() > 1) {
			do {
				String addStr = new String();
				addStr = c.getString(1) + "　" + c.getString(2);
				mAdapter.add(addStr);	
			} while (c.moveToPrevious());
			mainListView.setAdapter(mAdapter);
		}
		// finally close cursor.
		c.close();
	}

	private void startIndexListActivity() {
		Intent i = new Intent(this, AozoraBunkoIndexList.class);
		startActivityForResult(i, ActivityIndex);
	}

//	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
//		super.onListItemClick(l, v, position, id);
		
		Cursor c = this.mDbAdapter.fetchAllBookmarks();
		c.moveToPosition(c.getCount() - 1 - position);

		String authorName = c.getString(1);
		String worksName  = c.getString(2);
		String location   = c.getString(3);
		c.close();
		
		Intent i = new Intent(this, AozoraBunkoViewer.class);

		i.putExtra(AozoraBunkoViewer.KEY_AUTHORID,   -1);
		i.putExtra(AozoraBunkoViewer.KEY_LOCATION, location);
		i.putExtra(AozoraBunkoViewer.KEY_AUTHORNAME, authorName);
		i.putExtra(AozoraBunkoViewer.KEY_WORKSNAME, worksName);
		i.putExtra(AozoraBunkoViewer.KEY_BOOKMARKED, true);
		
		startActivity(i);
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean retval;
		retval = super.onCreateOptionsMenu(menu);
		menu.add(0, INDEX_ID, 0, R.string.menu_index);
		return retval;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean retval = false;
//		retval = super.onMenuItemSelected(featureId, item);

		switch (item.getItemId()) {
		case INDEX_ID:
			startIndexListActivity();
			break;
		}
		return retval;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		fillBookmarks();
	}

}
