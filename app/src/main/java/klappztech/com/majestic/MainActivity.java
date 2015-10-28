package klappztech.com.majestic;

/*
xlarge (xhdpi): 640x960
large (hdpi): 480x800
medium (mdpi): 320x480
small (ldpi): 240x320
 */
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String KEY_ROWID = "_id", DATABASE_TABLE = "routes_table" ;
    public static final String KEY_BUS_NUM = "bus_no",KEY_DEST = "dest",KEY_PLAT = "platform";
    public static final String[] ALL_KEYS = new String[] {KEY_ROWID, KEY_BUS_NUM, KEY_DEST, KEY_PLAT};
    DataBaseHelper myDbHelper;
    private int selected_count=0;

    TextView txtPlatform,txtPlatform2;
    ImageButton toggleShow;

    boolean isMaximized = true;

    // define the display assembly compass picture
    private ImageView image;


    CustomAutoCompleteView myAutoComplete;

    // adapter for auto-complete
    ArrayAdapter<String> myAdapter;
    // just to add some initial value0
    String[] item = new String[] {"Please search..."};
    private float currentDegree = 0f;
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleView(false);
        
        // database
        initDB();
        openDB();
        registerListClickCallback();

        //VIEW
        // autocompletetextview is in activity_main.xml
        myAutoComplete = (CustomAutoCompleteView) findViewById(R.id.myautocomplete);
        toggleShow = (ImageButton) findViewById(R.id.button);
        // add the listener so it will tries to suggest while the user types
        myAutoComplete.addTextChangedListener(new CustomAutoCompleteTextChangedListener(this));
        myAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //String selected_bus = (String) parent.getItemAtPosition(position);
                TextView txtBus = (TextView) view;

                String selected_bus = txtBus.getText().toString();

                //Toast.makeText(getApplicationContext(), "id value: " + selected_bus, Toast.LENGTH_SHORT).show();
                txtPlatform = (TextView) findViewById(R.id.textPlatform);
                txtPlatform2 = (TextView) findViewById(R.id.textViewPF2);


                String platform = getPlatFromFromBus(selected_bus);
                txtPlatform.setText(platform);
                txtPlatform2.setText(platform);

                toggleView(true);
                populateListViewFromDB(platform);

            }
        });

        // set our adapter
        myAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, item);
        myAutoComplete.setAdapter(myAdapter);

        //
        image = (ImageView) findViewById(R.id.imageView);
        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //button onclicklisterenters
        toggleShow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //load bookmarks

                toggleView(isMaximized = !isMaximized);


            }
        });
    }

    private void toggleView(boolean isVisible) {

        ListView LV = (ListView) findViewById(R.id.listView);
        TextView PF = (TextView) findViewById(R.id.textPlatform);
        TextView PF1 = (TextView) findViewById(R.id.textViewPF1);
        TextView PF2 = (TextView) findViewById(R.id.textViewPF2);
        ImageButton togglebtn = (ImageButton) findViewById(R.id.button);

        LinearLayout yellowBox = (LinearLayout) findViewById(R.id.yellowBox);


        if(!isVisible)  {
            LV.setVisibility(View.GONE);
            PF.setVisibility(View.GONE);
            PF2.setVisibility(View.VISIBLE);

            PF1.setVisibility(View.GONE);
            ViewGroup.LayoutParams params = PF1.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            PF1.setLayoutParams(params);

            togglebtn.setBackgroundResource(R.drawable.down);

        }  else  {
            LV.setVisibility(View.VISIBLE);
            PF.setVisibility(View.VISIBLE);
            PF2.setVisibility(View.GONE);

            PF1.setVisibility(View.VISIBLE);
            PF1.setText("PLATFORM");
            ViewGroup.LayoutParams params = PF1.getLayoutParams();
            params.width = ViewGroup.LayoutParams.FILL_PARENT;
            PF1.setLayoutParams(params);

            togglebtn.setBackgroundResource(R.drawable.up);
        }

        if(PF2.getText() =="")  {
            yellowBox.setVisibility(View.INVISIBLE);
        } else  {
            yellowBox.setVisibility(View.VISIBLE);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    private void registerListClickCallback() {
        ListView myList = (ListView) findViewById(R.id.listView);
        myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked,
                                    int position, long idInDB) {
                //Toast.makeText(getApplicationContext(), "Clicked on Item:" + idInDB, Toast.LENGTH_SHORT).show();
                ActionOnClick(idInDB);
            }
        });
    }

    private void ActionOnClick(long idInDB) {

        String bus = null, route=null;
        Cursor c = 	myDbHelper.myDataBase.query(true, DATABASE_TABLE, ALL_KEYS,
                KEY_ROWID+"='"+idInDB+"'" , null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        bus = c.getString(c.getColumnIndex(KEY_BUS_NUM));
        route = c.getString(c.getColumnIndex(KEY_DEST));

        //Toast.makeText(getApplicationContext(), bus + "\n"+route, Toast.LENGTH_SHORT).show();

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Bus # "+bus);
        alertDialog.setMessage(route);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();


    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);

        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        // how long the animation will take place
        ra.setDuration(210);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        image.startAnimation(ra);
        currentDegree = -degree;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
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
