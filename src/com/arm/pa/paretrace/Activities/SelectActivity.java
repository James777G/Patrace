package com.arm.pa.paretrace.Activities;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.arm.pa.paretrace.R;
import com.arm.pa.paretrace.Sources.CloudTraceResourceLoader;
import com.arm.pa.paretrace.Sources.UpdatableUI;
import com.arm.pa.paretrace.Types.Trace;
import com.arm.pa.paretrace.Util.Util;

public class SelectActivity extends Activity
{

	private static final String TAG = "PATRACE_UI";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    public static final String CONNECTION_UPDATE_ACTION = "com.arm.pa.paretrace.CONNECTION_STATUS_UPDATE";

	private static LinkedHashMap<String, String> mTraceOptionNames;

    private static boolean isRetracing = false;

    private static boolean isDownloading = false;
	private static LinkedHashMap<String, Boolean> mTraceOptionValues;

	private final ArrayList<Trace> mTraceList = new ArrayList<>();

    private ListView mListView;

	private View root_view;
	private BroadcastReceiver mConnectionStatusReceiver;

    private TraceListAdapter trace_list_adapter;

    private AlertDialog progressDialog;
    private final CloudTraceResourceLoader loader = new CloudTraceResourceLoader();

    public static String TESTING_DEVICE = "";

    public static String FILE_URL = "";

    public static boolean isTestMode = false;

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        AppCenter.start(getApplication(), "{\"03d43b93-4ecc-400c-9198-8d876b4e9042\"}",
                Analytics.class, Crashes.class);
        setContentView(R.layout.select_activity);
        Intent intent = getIntent();
        if(intent != null){
            if (intent.hasExtra("fileName")) {
                isRetracing = true;
            }
        }
        root_view = findViewById(R.id.root);

        trace_list_adapter = new TraceListAdapter(mTraceList);
        mListView = (ListView) findViewById(R.id.list);
        registerForContextMenu(mListView);
        mListView.setAdapter(trace_list_adapter);

        if(intent != null){
            if (intent.hasExtra("fileUri")) {
                CloudTraceResourceLoader.setFileUrl(intent.getStringExtra("fileUri"));
                FILE_URL = intent.getStringExtra("fileUri");
                isDownloading = true;
                Log.d(TAG, "The file URI is " + intent.getStringExtra("fileName"));
            }
        }
        setupTraceOptions();
        setupLayout();
        loadSettings();

        //Set receiver for status updates
    	if (mConnectionStatusReceiver == null){
    		final IntentFilter filter = new IntentFilter();
            filter.addAction(CONNECTION_UPDATE_ACTION);

            mConnectionStatusReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                	String status = intent.getStringExtra("status");
                	int progress = intent.getIntExtra("progress", 0);
                }
            };
            registerReceiver(mConnectionStatusReceiver, filter);
    	}


        if (intent != null) {
            // Check if the fileName extra is present
            if (intent.hasExtra("fileName")) {
                String filePath = intent.getStringExtra("fileName");

                Log.d(TAG, "File path received: " + filePath);

                startRetraceOperation(filePath);
            }

            if(intent.hasExtra("testDevice")) {
                TESTING_DEVICE = intent.getStringExtra("testDevice");
                isTestMode = true;
            }
        }



    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mConnectionStatusReceiver);
        /* Create a thread which will pause and then call exit */
        Thread suicideThread = new Thread() {
            public void run() {
                try {
                    sleep(500);
                }
                catch (InterruptedException ignored){
                }
                System.exit(0);
            }
        };
        suicideThread.start();
    }

    private void startRetraceOperation(String filePath) {
        for (int i = 0; i < mTraceList.size(); i++) {
            Log.i(TAG, mTraceList.get(i).getFile().getAbsolutePath());
            if (mTraceList.get(i).getFile().getAbsolutePath().equals(filePath)) {
                Log.i(TAG, "Path " + mTraceList.get(i).getFile().getAbsolutePath() + "Found, Start Retracing!");
                startRetrace(mTraceList.get(i));
                return;
            }
        }
        Log.e(TAG, "Invalid file path, no currently stored trace file detected");
    }

    private void scanFile(Context context, String path) {
        MediaScannerConnection.scanFile(context, new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {
            public void onScanCompleted(String path, Uri uri) {
                Log.i("ExternalStorage", "Scanned " + path + ":");
                Log.i("ExternalStorage", "-> uri=" + uri);
            }
        });
    }


    private void setupTraceOptions(){
    	mTraceOptionNames = new LinkedHashMap<>();
    	mTraceOptionNames.put("Offscreen", "forceOffscreen");
    	mTraceOptionNames.put("24bit Color", "use24BitColor");
    	mTraceOptionNames.put("Alpha", "useAlpha");
    	mTraceOptionNames.put("24bit Depth", "use24BitDepth");
    	mTraceOptionNames.put("AntiAlias (EGL)", "antialiasing");
    	mTraceOptionNames.put("Preload", "preload");
        mTraceOptionNames.put("Store Program Info", "storeProgramInfo");
        mTraceOptionNames.put("Remove Unused Attributes", "removeUnusedAttributes");
        mTraceOptionNames.put("Debug", "debug");
        mTraceOptionNames.put("Draw log", "drawlog");

    	mTraceOptionValues = new LinkedHashMap<String, Boolean>();
    	mTraceOptionValues.put("forceOffscreen", false);
    	mTraceOptionValues.put("use24BitColor", true);
    	mTraceOptionValues.put("useAlpha", true);
    	mTraceOptionValues.put("use24BitDepth", true);
    	mTraceOptionValues.put("antialiasing", false);
    	mTraceOptionValues.put("preload", false);
        mTraceOptionValues.put("storeProgramInfo", false);
        mTraceOptionValues.put("removeUnusedAttributes", false);
        mTraceOptionValues.put("debug", false);
        mTraceOptionValues.put("drawlog", false);
    }

    private void updateProgressDialog(int progress, float fileLengthMB) {
        if(fileLengthMB < 0) {
//            progressDialog.setTitle("The file is chunked. Unknown File Size");
        }
//        if(progress == 100) {
//            progressDialog.hide();
//        }
        if(isRetracing){
            return;
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage("Current Progress: " + progress + "%\nSize: " + fileLengthMB + " MB");
        } else {
            assert progressDialog != null;
           // progressDialog.show();
        }
    }

    private void setupTraceList() {
        mTraceList.clear();
        Log.i(TAG, "searching through inner storage");
        appendTracesInDir(new File("/data/local/tmp/apitrace"), mTraceList);
        Log.i(TAG, "searching through external storage");
        appendTracesInDir(new File(Util.getTraceFilePath()), mTraceList);
        appendTracesInDir(Objects.requireNonNull(getExternalCacheDir()), mTraceList);

        initializeProgressDialog(); // Initialize the progress dialog

        loader.downloadSingleTrace(mTraceList, new UpdatableUI() {
            @Override
            public void update() {
                trace_list_adapter.notifyDataSetChanged();
                progressDialog.setTitle("Download Finished");
                progressDialog.setMessage("");
            }

            @Override
            public void updateProgress(int progress, float fileLength) {
                float fileLengthMB = fileLength / (1024 * 1024); // Convert to MB if fileLength is in bytes
                updateProgressDialog(progress, fileLengthMB); // Update the progress dialog
            }

            @Override
            public void updateFailureMessage() {
                notifyNetworkFailure();
            }
        }, this);

        Collections.sort(mTraceList, Trace.fileNameComparator);
    }

    private void notifyNetworkFailure() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Network Error");
        builder.setMessage("Please Check Your Network Connectivity and Validity of the File URL");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void initializeProgressDialog() {
        if(isRetracing){
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Downloading Trace Files");
        builder.setMessage("Current Progress: 0%");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        progressDialog = builder.create();
        progressDialog.show();
    }

	private void setupLayout(){

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		for (Map.Entry<String, String> entry: mTraceOptionNames.entrySet()){
			mTraceOptionValues.put(entry.getValue(), pref.getBoolean("option_"+entry.getValue(), mTraceOptionValues.get(entry.getValue())));
		}

        // Trace files
		setupTraceList();

        mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				startRetrace((Trace)mListView.getAdapter().getItem(arg2));
			}
		});
//        root_view = findViewById(R.id.root);

		// Options
 		LinearLayout options_root = (LinearLayout) findViewById(R.id.retrace_options);
 		for (Map.Entry<String, String> entry: mTraceOptionNames.entrySet()){
 			addOption(options_root, entry.getKey());
 		}

	}

	private void addOption(LinearLayout v, final String option_name){
		LayoutInflater li = getLayoutInflater();
		@SuppressLint("InflateParams") View option_view = li.inflate(R.layout.option_item, null);
		((TextView)option_view.findViewById(R.id.option_title)).setText(option_name);
		((CheckBox)option_view.findViewById(R.id.option_checkbox)).setChecked(mTraceOptionValues.get(mTraceOptionNames.get(option_name)));
		((CheckBox)option_view.findViewById(R.id.option_checkbox)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mTraceOptionValues.put(mTraceOptionNames.get(option_name), ((CheckBox)arg0).isChecked());
			}
		});

		v.addView(option_view);
	}

	private void saveSettings(){
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

		for (Map.Entry<String, Boolean> entry: mTraceOptionValues.entrySet()){
			editor.putBoolean("option_"+entry.getKey(), entry.getValue());
		}
		try {
    		int thread = Integer.parseInt(((EditText)findViewById(R.id.option_thread_id)).getText().toString());
    		int alpha = Integer.parseInt(((EditText)findViewById(R.id.option_alpha)).getText().toString());
            boolean enable_res = ((CheckBox)findViewById(R.id.option_enableres)).isChecked();
            boolean enable_multithread = ((RadioButton)findViewById(R.id.option_enable_multithread)).isChecked();
            boolean force_single_window = ((RadioButton)findViewById(R.id.option_force_single_window)).isChecked();
            boolean enable_overlay = ((RadioButton)findViewById(R.id.option_enable_overlay)).isChecked();
            boolean enable_seltid = ((CheckBox)findViewById(R.id.option_enable_seltid)).isChecked();
            boolean enable_fullscreen = ((CheckBox)findViewById(R.id.option_enable_fullscreen)).isChecked();
    		int xres = Integer.parseInt(((EditText)findViewById(R.id.option_xres)).getText().toString());
    		int yres = Integer.parseInt(((EditText)findViewById(R.id.option_yres)).getText().toString());

			editor.putInt("settings_thread_id", thread);
			editor.putInt("settings_xres", xres);
			editor.putInt("settings_yres", yres);
			editor.putBoolean("settings_enable_res", enable_res);
			editor.putBoolean("settings_enable_seltid", enable_seltid);
			editor.putBoolean("settings_enable_multithread", enable_multithread);
			editor.putBoolean("settings_force_single_window", force_single_window);
			editor.putBoolean("settings_enable_overlay", enable_overlay);
			editor.putBoolean("settings_enable_fullscreen", enable_fullscreen);
			editor.putInt("settings_alpha", alpha);
		}
		catch (Exception e){
			Log.e(TAG, "Couldn't save settings");
		}

		editor.commit();
	}

	@SuppressLint("SetTextI18n")
    private void loadSettings(){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		/*
		for (int i = 0; i < TRACE_OPTIONS.length; i++){
			mTraceOptionsValues[i] = pref.getBoolean("settings_"+TRACE_OPTIONS[i], mTraceOptionsValues[i]);
		}
		*/

		int thread = pref.getInt("settings_thread_id", 0);
		int alpha = pref.getInt("settings_alpha", 100);
		int xres = pref.getInt("settings_xres", 1280);
		int yres = pref.getInt("settings_yres", 720);
        boolean enable_res = pref.getBoolean("settings_enable_res", false);
        boolean enable_seltid = pref.getBoolean("settings_enable_seltid", false);
		boolean enable_multithread = pref.getBoolean("settings_enable_multithread", false);
		boolean force_single_window = pref.getBoolean("settings_force_single_window", true);
		boolean enable_overlay = !force_single_window && pref.getBoolean("settings_enable_overlay", true);
		boolean enable_split   = !force_single_window && !enable_overlay;
		boolean enable_fullcreen = pref.getBoolean("settings_enable_fullcreen", false);

        ((RadioButton)root_view.findViewById(R.id.option_enable_singlethread)).setChecked(!enable_multithread);
		((EditText)root_view.findViewById(R.id.option_thread_id)).setText(""+thread);
        ((RadioButton)root_view.findViewById(R.id.option_enable_multithread)).setChecked(enable_multithread);
        ((RadioButton)root_view.findViewById(R.id.option_enable_overlay)).setChecked(enable_overlay);
        ((RadioButton)root_view.findViewById(R.id.option_enable_split)).setChecked(enable_split);
        ((RadioButton)root_view.findViewById(R.id.option_force_single_window)).setChecked(force_single_window);
		((EditText)root_view.findViewById(R.id.option_alpha)).setText(""+alpha);
        ((CheckBox)root_view.findViewById(R.id.option_enable_seltid)).setChecked(enable_seltid);
        ((EditText)root_view.findViewById(R.id.option_xres)).setText(""+xres);
        ((EditText)root_view.findViewById(R.id.option_yres)).setText(""+yres);
        ((CheckBox)root_view.findViewById(R.id.option_enableres)).setChecked(enable_res);
        ((CheckBox)root_view.findViewById(R.id.option_enable_fullscreen)).setChecked(enable_fullcreen);
	}

    private ArrayList<Trace> getTracesFromPath(String path){
    	File folder = new File(path);
        File[] listOfFilesData = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".pat");
            }
        });
        ArrayList<Trace> trace_list = new ArrayList<Trace>();
        if(listOfFilesData != null){
            for (File filepath : listOfFilesData) {
                Trace t = new Trace();
                t.setFile(filepath);
                trace_list.add(t);
            }
        }

        return trace_list;
    }

    private void appendTracesInDir(File file, ArrayList<Trace> traceList) {
        if (!file.isDirectory()) {
            Log.i(TAG, "The file type is not a directory");
            return;
        }
        Log.i(TAG, "The file type is a directory");
        String[] dirs = file.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });

        if (dirs != null) {
            for (String name : dirs)
                appendTracesInDir(new File(file, name), traceList);
        } else {
            Log.i(TAG, "No Inner Directories Detected");
        }

        File[] pats = file.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                Log.i(TAG, "The filename is " + name);
                return name.toLowerCase().endsWith(".pat");
            }
        });
        if (pats != null) {
            Log.i(TAG, pats.length + " Trace Files Found Under The Path");
            for (File curPat : pats) {
                Trace t = new Trace();
                t.setFile(curPat);
                traceList.add(t);
            }
        } else {
            Log.i(TAG, "no .pat files detected");
        }
        trace_list_adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
        ContextMenuInfo menuInfo) {
      if (v.getId()==R.id.list) {
        menu.setHeaderTitle("Trace file options");
        String[] menuItems = { "Delete" };
        for (int i = 0; i<menuItems.length; i++) {
          menu.add(Menu.NONE, i, i, menuItems[i]);
        }
      }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
      AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
      if (item.getTitle().equals("Delete")){
    	  if (info.position < mTraceList.size() ){
    		  if (mTraceList.get(info.position).delete()){
    		    Toast.makeText(SelectActivity.this, "File deleted successfully.", Toast.LENGTH_LONG).show();
    			  mTraceList.remove(info.position);
    			  ((TraceListAdapter)mListView.getAdapter()).notifyDataSetChanged();
    		  }
    		  else {
    			  Toast.makeText(SelectActivity.this, "Couldn't delete file.", Toast.LENGTH_LONG).show();
    		  }
    	  }
      }

      return true;
    }

    private class TraceListAdapter extends BaseAdapter {

    	private final ArrayList<Trace> mTraceList;
    	private final LayoutInflater mInflater;

    	public TraceListAdapter(ArrayList<Trace> list) {
    		mTraceList = list;
    		mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		}

		@Override
		public int getCount() {
			return mTraceList.size();
		}

		@Override
		public Trace getItem(int position) {
			return mTraceList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressLint("SetTextI18n")
        @Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null){
				convertView = mInflater.inflate(R.layout.trace_list_item, null);
			}
			Trace trace = mTraceList.get(position);
			TextView title = (TextView) convertView.findViewById(R.id.title);
			title.setText(trace.getFileName());
			TextView subtitle = (TextView) convertView.findViewById(R.id.subtitle);
			subtitle.setText("("+trace.getFileSize() + ")  " + trace.getFileDir());

			return convertView;
		}
    }


    void startRetrace(Trace trace)
    {
        Intent intent = new Intent(this, RetraceActivity.class);
        intent.putExtra("fileName", trace.getFile().getAbsolutePath());
        intent.putExtra("isGui", true);

        boolean enable_snapshot = ((CheckBox)findViewById(R.id.option_enablesnapshot)).isChecked();
        if (enable_snapshot) {
            intent.putExtra("callset", ((EditText)findViewById(R.id.option_snapshotcallset)).getText().toString());
            intent.putExtra("callsetprefix", ((EditText)findViewById(R.id.option_snapshotcallsetprefix)).getText().toString());
        }

        boolean enable_framerange = ((CheckBox)findViewById(R.id.option_enableframerange)).isChecked();
        if (enable_framerange) {
            int fstart = Integer.parseInt(((EditText)findViewById(R.id.option_framestart)).getText().toString());
            int fend = Integer.parseInt(((EditText)findViewById(R.id.option_frameend)).getText().toString());
            intent.putExtra("frame_start", fstart);
            intent.putExtra("frame_end", fend);
        }

        boolean enable_multithread = false;
        try {
            enable_multithread = ((RadioButton)findViewById(R.id.option_enable_multithread)).isChecked();
            if (enable_multithread) {
                intent.putExtra("multithread", true);
            }
            else {
                intent.putExtra("multithread", false);
                boolean seltid = ((CheckBox)findViewById(R.id.option_enable_seltid)).isChecked();
                int thread = Integer.parseInt(((EditText)findViewById(R.id.option_thread_id)).getText().toString());
                if (seltid) {
                    intent.putExtra("tid", thread);
                }
            }
        }
        catch (NumberFormatException e){
            Toast.makeText(this, "Invalid thread id, frame start or frame end number." , Toast.LENGTH_SHORT).show();
            return;
        }

        boolean force_single_window = ((RadioButton)findViewById(R.id.option_force_single_window)).isChecked();
        if (force_single_window && enable_multithread){
            Toast.makeText(this, "Force single window is not valid when multithread enabled!" , Toast.LENGTH_SHORT).show();
            return;
        }
        if (force_single_window)
        {
            intent.putExtra("force_single_window", true);
            intent.putExtra("enOverlay", false);
        }
        else
        {
            boolean enable_overlay = ((RadioButton)findViewById(R.id.option_enable_overlay)).isChecked();
            intent.putExtra("force_single_window", false);
            if (enable_overlay)
            {
                intent.putExtra("enOverlay", true);
                int alpha = Integer.parseInt(((EditText)findViewById(R.id.option_alpha)).getText().toString());
                intent.putExtra("transparent", alpha);
            }
            else
            {
                intent.putExtra("enOverlay", false);
            }
        }

        boolean enable_FullScreen = ((CheckBox)findViewById(R.id.option_enable_fullscreen)).isChecked();
        if (enable_FullScreen)
        {
            intent.putExtra("enFullScreen", true);
        }
        else
        {
            intent.putExtra("enFullScreen", false);
        }

        boolean enable_res = ((CheckBox)findViewById(R.id.option_enableres)).isChecked();
        if (enable_res) {
            try {
                int xres = Integer.parseInt(((EditText)findViewById(R.id.option_xres)).getText().toString());
                int yres = Integer.parseInt(((EditText)findViewById(R.id.option_yres)).getText().toString());
                intent.putExtra("oresW", xres);
                intent.putExtra("oresH", yres);
            }
            catch (NumberFormatException e){
                Toast.makeText(this, "Invalid resolution. Please enter an integer" , Toast.LENGTH_SHORT).show();
                return;
            }
        }

        for (Map.Entry<String, Boolean> entry: mTraceOptionValues.entrySet()){
            intent.putExtra(entry.getKey(), entry.getValue());
        }

        this.startActivity(intent);
    }

}
