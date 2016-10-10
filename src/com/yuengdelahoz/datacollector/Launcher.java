package com.yuengdelahoz.datacollector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Launcher extends Activity {
	private TextView first_name;
	private TextView last_name;
	private TextView birth_date;
	private RadioGroup sex;
	private TextView height;
	private TextView weight;
	private View b;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_info);
		first_name = (TextView) findViewById(R.id.firstname);
		last_name = (TextView) findViewById(R.id.lastname);
		birth_date = (TextView) findViewById(R.id.birth_date);
		sex = (RadioGroup) findViewById(R.id.sex);
		height = (TextView) findViewById(R.id.height);
		weight = (TextView) findViewById(R.id.weight);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.user_info, menu);
		return true;
	}

	public void mSubmit(View view) {
		int mult = first_name.getText().length() * last_name.getText().length()
				* birth_date.getText().length() * height.getText().length()
				* weight.getText().length();
		if (mult == 0) {
			Toast.makeText(getApplicationContext(),
					"Please fill out all fields", Toast.LENGTH_SHORT).show();
		} else {

			int radioButtonID = sex.getCheckedRadioButtonId();
			RadioButton radioButton = (RadioButton) sex
					.findViewById(radioButtonID);
			String selection = (String) radioButton.getText();
			Log.i("Radio Button", selection);
			
			Date date = new Date();


			String[] userdata = new String[8];
			userdata[0] = "" + first_name.getText();
			userdata[1] = "" + last_name.getText();
			userdata[2] = "" + birth_date.getText();
			userdata[3] = "" + selection;
			userdata[4] = "" + height.getText();
			userdata[5] = "" + weight.getText();
			String datemili = ""+date.getTime();
			String dirName = "Patient_"+first_name.getText()+"_"+datemili;
			userdata[6] = dirName;
			BufferedWriter output;

			try {
				File dir = getAlbumStorageDir(dirName);
				File file = new File(dir, "user_"+userdata[0]);
		        FileWriter writer = new FileWriter(file,true);
		        output = new BufferedWriter(writer);
		        output.append("First Name : "+first_name.getText()+"\n");
		        output.append("Last Name : "+last_name.getText()+"\n");
		        output.append("Birth Date : "+birth_date.getText()+"\n");
		        output.append("Sex : "+selection+"\n");
		        output.append("Height : "+height.getText()+"\n");
		        output.append("Weight : "+weight.getText()+"\n");
		        output.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Intent intent = new Intent(this, Activity_Selection.class);
			intent.putExtra("UserData", userdata);
			startActivity(intent);
		}

	}

	public void showDatePickerDialog(View v) {
		DatePickerFragment nFrag = new DatePickerFragment();
		nFrag.show(getFragmentManager(), "datePicker");
	}

	static public File getAlbumStorageDir(String albumName) {
		// Get the directory for the user's public pictures directory.
		File file = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
				albumName);
		if (!file.mkdirs()) {
			Log.e("Error", "Directory not created");
		}
		return file;
	}

}
