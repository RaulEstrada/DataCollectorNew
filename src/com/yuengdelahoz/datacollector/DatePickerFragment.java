package com.yuengdelahoz.datacollector;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.TextView;

public class DatePickerFragment extends DialogFragment
	    implements DatePickerDialog.OnDateSetListener {
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current date as the default date in the picker
		final Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		
		// Create a new instance of DatePickerDialog and return it
		return new DatePickerDialog(getActivity(), this, year, month, day);
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		TextView tv = (TextView) getActivity().findViewById(R.id.birth_date);
		int month = monthOfYear+1;
		Log.i("Month", ""+monthOfYear);
		tv.setText(String.format("%02d", month)+"/"+String.format("%02d", dayOfMonth)+"/"+String.format("%02d", year));
		
	}
}