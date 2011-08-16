package de.jrx.ad.nodes;

import android.net.Uri;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


public class NodesBookmarks extends ListActivity {
	private DatabaseHelper db=null;
	public static Cursor bookmarksCursor=null;	
	
	private static final int DELETE_ID = Menu.FIRST+1;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		db = new DatabaseHelper(this);
		bookmarksCursor=db
			.getReadableDatabase()
			.rawQuery("SELECT _ID, name, uri "+
			"FROM bookmarks ORDER BY name",
			null);	
		
		setContentView(R.layout.viewlist_bookmarks);
		setListAdapter(new Adapter());
		registerForContextMenu(getListView());
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();

		bookmarksCursor.close();
		db.close();
	}
	
	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
			
		TextView uri_label=(TextView)v.findViewById(R.id.uri);
		
		Uri uri=Uri.parse(uri_label.getText().toString());
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
		//startActivity(new Intent(this, UriHandler.class));
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
		ContextMenu.ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, DELETE_ID, Menu.NONE, "Delete")
				.setAlphabeticShortcut('d');
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case DELETE_ID:
				AdapterView.AdapterContextMenuInfo info=
					(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

				deleteNode(info.id);
				return(true);
		}

		return(super.onOptionsItemSelected(item));
	}
	
	class Adapter extends SimpleCursorAdapter {
		Adapter() {
			super(NodesBookmarks.this,
				R.layout.row_bookmarks, bookmarksCursor,
				new String[] {DatabaseHelper.NAME, DatabaseHelper.URI},
				new int[] {R.id.title, R.id.uri}
				);
		}

		@Override
		public void bindView(View row, Context context, Cursor c) {
			
			TextView label=(TextView)row.findViewById(R.id.title);
			label.setText(c.getString(c.getColumnIndex(DatabaseHelper.NAME)));
			
			TextView uri=(TextView)row.findViewById(R.id.uri);
			uri.setText(c.getString(c.getColumnIndex(DatabaseHelper.URI)));						
		}
		
	}
	
	public void addNode(View v) {
		LayoutInflater inflater=LayoutInflater.from(this);
		View addView=inflater.inflate(R.layout.list_add, null);
		final DialogWrapper wrapper=new DialogWrapper(addView);
		
		new AlertDialog.Builder(this)
		.setTitle(R.string.list_add_name)
		.setView(addView)
		.setPositiveButton(R.string.button_ok,
			new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,
				int whichButton) {
					processAdd(wrapper);
				}
		})
		.setNegativeButton(R.string.button_cancel,
			new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,
				int whichButton) {
					// ignore, just dismiss
				}
		})
		.show();
	}

	private void deleteNode(final long rowId) {
		if (rowId>0) {
			new AlertDialog.Builder(this)
			.setTitle(R.string.list_delete_name)
			.setPositiveButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
					int whichButton) {
						processDelete(rowId);
					}
			})
			.setNegativeButton(R.string.button_cancel,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
					int whichButton) {
						// ignore, just dismiss
					}
			})
			.show();
		}
	}	
	private void processAdd(DialogWrapper wrapper) {
		ContentValues values=new ContentValues(2);

		values.put(DatabaseHelper.NAME, wrapper.getName());
		values.put(DatabaseHelper.URI, wrapper.getUri());

		db.getWritableDatabase().insert("bookmarks", DatabaseHelper.NAME, values);
		bookmarksCursor.requery();
	}
	
	private void processDelete(long rowId) {
		String[] args={String.valueOf(rowId)};

		db.getWritableDatabase().delete("bookmarks", "_ID=?", args);
		bookmarksCursor.requery();
	}
	
	class DialogWrapper {
		EditText nameField=null;
		EditText uriField=null;
		View base=null;

	
		DialogWrapper(View base) {
			this.base=base;
			uriField=(EditText)base.findViewById(R.id.uri);
		}

		String getName() {
			return(getNameField().getText().toString());
		}

		String getUri() {
			return(getUriField().getText().toString());
		}

		private EditText getNameField() {
			if (nameField==null) {
				nameField=(EditText)base.findViewById(R.id.name);
			}

			return(nameField);
		}

		private EditText getUriField() {
			if (uriField==null) {
				uriField=(EditText)base.findViewById(R.id.uri);
			}

			return(uriField);
		}
		
	}
}