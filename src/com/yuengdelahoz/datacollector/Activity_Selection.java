package com.yuengdelahoz.datacollector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.w3c.dom.UserDataHandler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_Selection extends Activity {


	private TextView trial;
	private String dirName;
	private String[] userData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trial_selector);
		trial = (TextView) findViewById(R.id.trial);
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		userData= bundle.getStringArray("UserData");
		dirName = userData[6];
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.trial_selector, menu);
		return true;
	}
	
	public void Start_Trial (View v){
		if (trial.getText().length()==0){
			Toast.makeText(getApplicationContext(),
					"Please fill out all fields", Toast.LENGTH_SHORT).show();
		}
		else {
			String trial_ = "trial_"+trial.getText()+"_"+new Date().getTime()+".csv";
			userData[7] = trial_;
			BufferedWriter output;

			try {
				File dir = Launcher.getAlbumStorageDir(dirName);
				File file = new File(dir, trial_);
		        FileWriter writer = new FileWriter(file,true);
		        output = new BufferedWriter(writer);
		        output.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Intent intent = new Intent(this, Data_Collection.class);
			intent.putExtra("UserData", userData);
			startActivity(intent);
		}
		
	}

}
