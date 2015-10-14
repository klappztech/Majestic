package klappztech.com.majestic;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_ROWID = "_id", DATABASE_TABLE = "routes_table" ;
    public static final String KEY_BUS_NUM = "bus_no",KEY_DEST = "dest",KEY_PLAT = "platform";
    public static final String[] ALL_KEYS = new String[] {KEY_ROWID, KEY_BUS_NUM, KEY_DEST, KEY_PLAT};
    DataBaseHelper myDbHelper;
    private int selected_count=0;

    TextView txtPlatform,txtBus1, txtBus2,txtBus3,txtBus4,txtBus5;

    CustomAutoCompleteView myAutoComplete;

    // adapter for auto-complete
    ArrayAdapter<String> myAdapter;
    // just to add some initial value
    String[] item = new String[] {"Please search..."};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // database
        initDB();
        openDB();



        // autocompletetextview is in activity_main.xml
        myAutoComplete = (CustomAutoCompleteView) findViewById(R.id.myautocomplete);

        // add the listener so it will tries to suggest while the user types
        myAutoComplete.addTextChangedListener(new CustomAutoCompleteTextChangedListener(this));
        myAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //String selected_bus = (String) parent.getItemAtPosition(position);
                TextView txtBus = (TextView) view;

                String selected_bus = txtBus.getText().toString();

                Toast.makeText(getApplicationContext(), "id value: " +selected_bus , Toast.LENGTH_SHORT).show();
                txtPlatform = (TextView) findViewById(R.id.textPlatform);


                String platform = getPlatFromFromBus(selected_bus);
                txtPlatform.setText(platform);

                populateListViewFromDB(platform);

            }
        });

        // set our adapter
        myAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, item);
        myAutoComplete.setAdapter(myAdapter);
    }

    public String getPlatFromFromBus(String selected_bus) {
        String pf = null;
        Cursor c = 	myDbHelper.myDataBase.query(true, DATABASE_TABLE, ALL_KEYS,
                KEY_BUS_NUM+"='"+selected_bus+"'" , null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        pf = c.getString(c.getColumnIndex(KEY_PLAT));

        return pf;
    }

    private void openDB() {
        myDbHelper.myDataBase = myDbHelper.myDataBase;
    }

    private void initDB() {

        myDbHelper = new DataBaseHelper(this);

        try {
            myDbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }
        try {
            myDbHelper.openDataBase();
        }catch(SQLException sqle){
            throw sqle;
        }
    }

    public Cursor getRow(long rowId) {
        String where = KEY_ROWID + "=" + rowId;
        Cursor c = 	myDbHelper.myDataBase.query(true, DATABASE_TABLE, ALL_KEYS,
                where, null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }
    public Cursor getAllRows() {
        String where = null;
        Cursor c = 	myDbHelper.myDataBase.query(true, DATABASE_TABLE, ALL_KEYS,
                where, null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    public Cursor getAllRowsWhere(String where) {
        //where = null;
        Cursor c = 	myDbHelper.myDataBase.query(true, DATABASE_TABLE, ALL_KEYS,
                where, null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    public void populateListViewFromDB(String platform_no) {
        // Set the adapter for the list view
        final ListView myList = (ListView) findViewById(R.id.listView);
        View empty = findViewById(R.id.emptyView);

        myList.setEmptyView(empty);

        Cursor cursor = getAllRowsWhere(KEY_PLAT+" = '"+platform_no+"'");

        // Allow activity to manage lifetime of the cursor.
        // DEPRECATED! Runs on the UI thread, OK for small/short queries.
        startManagingCursor(cursor);

        // Setup mapping from cursor to view fields:
        String[] fromFieldNames = new String[]
                {KEY_BUS_NUM, KEY_DEST};
        int[] toViewIDs = new int[]
                {R.id.item_name, R.id.item_otp};

        // Create adapter to may columns of the DB onto elemesnt in the UI.
        SimpleCursorAdapter myCursorAdapter =
                new SimpleCursorAdapter(
                        this,        // Context
                        R.layout.item_layout,    // Row layout template
                        cursor,                    // cursor (set of DB records to map)
                        fromFieldNames,            // DB Column names
                        toViewIDs                // View IDs to put information in
                );

        myList.setAdapter(myCursorAdapter);


    }

    // this function is used in CustomAutoCompleteTextChangedListener.java
    public String[] getItemsFromDb(String searchTerm){

        // add items on the array dynamically
        List<MyObject> products = myDbHelper.read(searchTerm);
        int rowCount = products.size();

        String[] item = new String[rowCount];
        int x = 0;

        for (MyObject record : products) {

            item[x] = record.objectName;
            x++;
        }

        return item;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myDbHelper.close();
    }
}
