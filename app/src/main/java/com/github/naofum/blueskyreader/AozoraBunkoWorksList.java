package com.github.naofum.blueskyreader;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AozoraBunkoWorksList extends AppCompatActivity {

	public static final String KEY_AUTHORID   = "AUTHORID";
	public static final String KEY_AUTHORNAME = "AUTHORNAME";
	private AozoraReaderWorksDbAdapter mDbAdapter;
	private long authorId;
	private String authorName;

	private ListView worksListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.works_list);
		worksListView = (ListView)findViewById(R.id.worksListView);
		worksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView parentView, View childView,
									int position, long id) {
				onListItemClick(worksListView, childView, position, id);
			}
		});

		// Pick Up bundle.
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			this.authorId = extras.getLong(AozoraBunkoWorksList.KEY_AUTHORID);
			this.authorName = extras.getString(AozoraBunkoWorksList.KEY_AUTHORNAME);
			String title = getString(R.string.works_list) + "(" + this.authorName + ")/" + getString(R.string.app_name);
			setTitle(title);

			this.mDbAdapter = new AozoraReaderWorksDbAdapter(this);
			this.mDbAdapter.open();
			
			Cursor worksListCursor = null;
			
			worksListCursor = this.mDbAdapter.fetchWorksList(this.authorId);
			if (worksListCursor.getCount() == 0) {
				worksListCursor.close();
				this.mDbAdapter.updateWorksDB(this.authorId);
				worksListCursor = this.mDbAdapter.fetchWorksList(this.authorId);
			}
			
			ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, R.layout.works_list, R.id.works_row);
			
			if (worksListCursor != null) {
				startManagingCursor(worksListCursor);
				worksListCursor.moveToFirst();
				do {
					String addStr = new String();
					addStr = worksListCursor.getString(1)
						+ "　（"
						+ this.mDbAdapter.getKanazukaiType(worksListCursor.getLong(2))
						+ "）";
					mAdapter.add(addStr);									
				} while (worksListCursor.moveToNext());
			}

			worksListView.setAdapter(mAdapter);
		}
	}

	@Override
	protected void onDestroy() {
		this.mDbAdapter.close();
		super.onDestroy();
	}
	
//	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
//		super.onListItemClick(l, v, position, id);
		Cursor c = this.mDbAdapter.fetchWorksInfoFromPosition((long)position, this.authorId);
		
		long worksId = c.getLong(1);
		String title = c.getString(2);
		String location = c.getString(3);
		
		c.close();
		
		Intent i = new Intent(this, AozoraBunkoViewer.class);
		i.putExtra(AozoraBunkoViewer.KEY_AUTHORID,   this.authorId);
		i.putExtra(AozoraBunkoViewer.KEY_AUTHORNAME, this.authorName);
		i.putExtra(AozoraBunkoViewer.KEY_WORKSID, worksId);
		i.putExtra(AozoraBunkoViewer.KEY_WORKSNAME , title);
		i.putExtra(AozoraBunkoViewer.KEY_LOCATION, location);
		i.putExtra(AozoraBunkoViewer.KEY_BOOKMARKED, false);
		
		startActivity(i);
	}

}
