package com.proinlab.gplustest;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.plus.Moments;
import com.google.android.gms.plus.Moments.LoadMomentsResult;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.People.LoadPeopleResult;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusShare;
import com.google.android.gms.plus.model.moments.MomentBuffer;
import com.google.android.gms.plus.model.people.PersonBuffer;

public class MainActivity extends Activity {

	private GoogleApiClient mGoogleApiClient;

	private static final int REQUEST_RESOLVE_ERROR = 1001;
	private boolean mResolvingError = false;

	private static final String STATE_RESOLVING_ERROR = "resolving_error";

	private SignInButton signInButton;
	private TextView printData;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		printData = (TextView) findViewById(R.id.loaded_data);

		mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN).addConnectionCallbacks(connectionCallbacks)
				.addOnConnectionFailedListener(connectionFailedListener).build();
		mResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

		signInButton = (SignInButton) findViewById(R.id.sign_in_button);
		signInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mResolvingError) {
					mGoogleApiClient.connect();
				}
			}
		});

		Button shareButton = (Button) findViewById(R.id.share_button);
		shareButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent shareIntent = new PlusShare.Builder(MainActivity.this).setType("text/plain").setText("Welcome to the Google+ platform.")
						.setContentUrl(Uri.parse("https://developers.google.com/+/")).getIntent();

				startActivityForResult(shareIntent, 0);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		mGoogleApiClient.disconnect();
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_RESOLVE_ERROR) {
			mResolvingError = false;
			if (resultCode == RESULT_OK) {
				if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
					mGoogleApiClient.connect();
				}
			}
		}
	}

	private GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
		@Override
		public void onConnectionSuspended(int cause) {

		}

		@Override
		public void onConnected(Bundle connectionHint) {
			printData.setText(Plus.AccountApi.getAccountName(mGoogleApiClient));

			Plus.PeopleApi.loadVisible(mGoogleApiClient, null).setResultCallback(new ResultCallback<People.LoadPeopleResult>() {
				@Override
				public void onResult(LoadPeopleResult peopleData) {
					if (peopleData.getStatus().getStatusCode() == CommonStatusCodes.SUCCESS) {
						PersonBuffer personBuffer = peopleData.getPersonBuffer();
						try {
							int count = personBuffer.getCount();
							printData.setText(printData.getText() + "\n\npersons");
							for (int i = 0; i < count; i++) {
								printData.setText(printData.getText() + "\n" + personBuffer.get(i).getDisplayName());
							}
						} finally {
							personBuffer.close();
						}
					} else {
						Log.e("TAG", "Error requesting visible circles: " + peopleData.getStatus());
					}
				}
			});
		}
	};

	private GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			if (mResolvingError) {
				return;
			} else if (result.hasResolution()) {
				try {
					mResolvingError = true;
					result.startResolutionForResult(MainActivity.this, REQUEST_RESOLVE_ERROR);
				} catch (SendIntentException e) {
					mGoogleApiClient.connect();
				}
			} else {
				Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
				mResolvingError = true;
			}
		}
	};

}
