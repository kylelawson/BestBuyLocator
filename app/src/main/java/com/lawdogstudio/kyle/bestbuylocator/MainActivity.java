package com.lawdogstudio.kyle.bestbuylocator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.batch.android.Batch;
import com.batch.android.Config;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.gitonway.lee.niftymodaldialogeffects.lib.Effectstype;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import io.fabric.sdk.android.Fabric;

//Implements the bottom sheet's interface so that something can be done with the data received
public class MainActivity extends AppCompatActivity implements BottomSheetFragment.onSetListener, OnMapReadyCallback, JSONFragment.onSelectionListener {

    private static final int TWEET_COMPOSER_REQUEST_CODE = 0;
    private static final String GITHUB_URL = "https://github.com/kylelawson/BestBuyLocator";

    //The floating action button, try again button, and instruction text
    FloatingActionButton fab;
    TextView initialTv;
    Button tryAgain;

    //Facebook stuff
    ImageButton shareButton;
    ShareDialog shareDialog;
    CallbackManager callbackManager;
    Boolean fbClicked = false;
    Boolean shared = false;
    String location;

    //Twitter stuff
    ImageButton twitterButton;
    public static final String TWITTER_KEY = "AxmudfMeKMhIx2IzrxP6052Ku";
    public static final String TWITTER_SECRET = "bXWpfIaD5oAvuL7lWGIMUlkJ37yLF8Ku4vlnhLEO2GMv6OwlRN";
    Boolean twitterClicked = false;

    //Private variables for try again button
    private String zip;
    private String rad;

    //Private variables for the address, latitude and longitude
    private double latitude;
    private double longitude;
    private String add;

    //Set a fragment manager for the main screen fragment
    FragmentManager fm = getSupportFragmentManager();
    Fragment listFragment;
    SupportMapFragment mapFragment;

    //Google Map
    private GoogleMap gMap;

    //Firebase Database
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference facebookDBRef = database.getReference("facebookposts");
    DatabaseReference twitterDBRef = database.getReference("twitterposts");

    //For the dialog effects
    NiftyDialogBuilder alreadyPostedDialog;

    ImageButton github;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setup Batch Push
        Batch.Push.setGCMSenderId("804237166313");

        // TODO : switch to live Batch Api Key before shipping
        Batch.setConfig(new Config("DEV57451DBA54227C95AB023791434")); // development
        // Batch.setConfig(new Config("57451DBA50E6EFEBF616BCA5D4574E")); // live

        //Initiate the fragments themselves by attaching them to the fragment manager and assigning it the
        //layout within the activity_main.xml
        initiateFragments();

        initialTv = (TextView) findViewById(R.id.initial_fb_text);

        //The try again button in case no internet is available, the user can try again without
        //having to input the search criteria again
        tryAgain = (Button) findViewById(R.id.try_again_button);
        tryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Make the button disappear
                tryAgain.setVisibility(View.GONE);

                //Call the interface to attempt to update the listview again
                setSearchCriteria(zip, rad);
            }
        });

        //Floating action button initialization
        fab = (FloatingActionButton) findViewById(R.id.floating_action_button);

        //Floating action button listener that opens the modal bottomsheet dialog fragment view
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                BottomSheetDialogFragment bottomSheetDialogFragment = new BottomSheetFragment();
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                initialTv.setVisibility(View.GONE);

            }
        });

        //Facebook button and wiring initialization
        facebookShareButtonInitiate();

        //Twitter button and wiring initialization
        twitterShareButtonInitiate();

        //Setup Github Button
        github = (ImageButton) findViewById(R.id.github_button);
        github.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(GITHUB_URL));
                startActivity(i);
            }
        });

        //Restore Instance State if able
        if(savedInstanceState != null){
            String lat = String.valueOf(savedInstanceState.getDouble("lat"));
            String lon = String.valueOf(savedInstanceState.getDouble("long"));
            String add = savedInstanceState.getString("add");
            setMapLocation(lat,lon, true, add);
        }
    }

    private void initiateFragments(){
        listFragment = fm.findFragmentById(R.id.list_fragment_container);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        //If the fragment is empty, attach a fragment activity class to it and begin a fragment transaction
        //in order to show the fragment in the main view
        if(listFragment == null){
            listFragment = new JSONFragment();
            fm.beginTransaction().add(R.id.list_fragment_container, listFragment).commit();
        }

        if(mapFragment == null){
            mapFragment.getMapAsync(this);
            gMap = mapFragment.getMap();

            double lat = 39.5;
            double lng = -98.35;

            CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(lat,lng)).zoom(17).build();
            GoogleMapOptions googleMapOptions = new GoogleMapOptions().camera(cameraPosition);
            SupportMapFragment.newInstance(googleMapOptions);

        }
    }

    //Override the implemented interface so that the data can be sent to the JSON fragment
    @Override
    public void setSearchCriteria(String zipCodeIn, String radiusIn) {

        //Uses bundle to send data
        Bundle bundle = new Bundle();
        bundle.putString("zip", zipCodeIn);
        bundle.putString("radius", radiusIn);

        //Apply values to private variables for try again button
        zip = zipCodeIn;
        rad = radiusIn;

        //Instantiate a new JSONFragment that holds the bundle
        Fragment replacement = new JSONFragment();
        replacement.setArguments(bundle);

        //Use the fragment manager to replace the current JSON Fragment with the new one that has arguments
        fm.beginTransaction().replace(R.id.list_fragment_container, replacement).commit();

    }


    //Interface that is used to move the map to the selected location in the listview
    @Override
    public void setMapLocation(String latIn, String lngIn, Boolean rotation, String address) {

        //Parse the strings received from the given listview location data
        latitude = Double.parseDouble(latIn);
        longitude = Double.parseDouble(lngIn);
        add = address;
        location = "" + latitude + longitude;

        //Connects the Google Map object to the supportMapFragment so the map can be manipulated
        gMap = mapFragment.getMap();

        //Clears any existing markers
        gMap.clear();

        //Create a marker on the location selected
        MarkerOptions marker = new MarkerOptions().position(new LatLng(latitude, longitude));
        gMap.addMarker(marker);

        //Move the map to the marker
        CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom(15f).build();
        if(!rotation) {
            gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }else{
            gMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        //Makes the Facebook button visible and onClickListener
        shareButton.setVisibility(View.VISIBLE);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences pref = getSharedPreferences("POSTED_STORES", MODE_PRIVATE);
                int count = pref.getInt("COUNT", 0);

                if(count != 0){
                    for(int i = 0; i < count; i++){
                        if(pref.getString("store" + i, "0").equals(location)){
                            alreadyPostedSocialMedia("posted");
                            shared = true;

                        }
                    }

                    if(shared == false){
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("store" + (count + 1), location);
                        editor.putInt("COUNT", count + 1);

                        editor.commit();
                        //Uses ShareLinkContent object to build the context for the share dialog
                        ShareLinkContent linkContent = new ShareLinkContent.Builder().build();

                        //Show Facebook share dialog
                        shareDialog.show(linkContent);
                    }

                }else{
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("store" + (count + 1), location);
                    editor.putInt("COUNT", count + 1);

                    editor.commit();
                    //Uses ShareLinkContent object to build the context for the share dialog
                    ShareLinkContent linkContent = new ShareLinkContent.Builder().build();

                    //Show Facebook share dialog
                    shareDialog.show(linkContent);
                }

                /*
                facebookDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                            if(String.valueOf(dsp.getValue()).equals(location)){
                                shared = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });

                //Because it's against Facebook policy to prefill text in a post, a real website URL
                //Could be placed here with the post
                if (shared) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Already Posted")
                            .setMessage("You've already posted that you've purchased a new Smart Homes Inc." +
                                    "thermostat")
                            .show();

                } else {
                    fbClicked = true;
                    shared = false;

                    //Uses ShareLinkContent object to build the context for the share dialog
                    ShareLinkContent linkContent = new ShareLinkContent.Builder().build();

                    //Show Facebook share dialog
                    shareDialog.show(linkContent);
                }*/
                shared = false;
            }

        });

        //Makes the Twitter button visible and onClickListener
        twitterButton.setVisibility(View.VISIBLE);
        twitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences pref = getSharedPreferences("TWEETED_STORES", MODE_PRIVATE);
                int count = pref.getInt("COUNT", 0);

                if(count != 0){
                    for(int i = 0; i < count; i++){
                        if(pref.getString("store" + i, "0").equals(location)){
                            alreadyPostedSocialMedia("tweeted");
                            shared = true;

                        }
                    }

                    if(shared == false){
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("store" + (count + 1), location);
                        editor.putInt("COUNT", count + 1);

                        editor.commit();

                        //TweetComposer.Builder builder = new TweetComposer.Builder(MainActivity.this)
                        //        .text("I got my Smart Homes Thermostat at Best Buy Located at " + add);
                        //builder.show();
                        Intent intent = new TweetComposer.Builder(MainActivity.this)
                                .text("I got my Smart Homes Thermostat at Best Buy Located at " + add)
                                .createIntent();

                        twitterClicked = true;

                        startActivityForResult(intent, TWEET_COMPOSER_REQUEST_CODE);

                        shared = false;
                    }

                }else{

                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("store" + (count + 1), location);
                    editor.putInt("COUNT", count + 1);

                    editor.commit();

                    //TweetComposer.Builder builder = new TweetComposer.Builder(MainActivity.this)
                    //        .text("I got my Smart Homes Thermostat at Best Buy Located at " + add);
                    //builder.show();
                    Intent intent = new TweetComposer.Builder(MainActivity.this)
                            .text("I got my Smart Homes Thermostat at Best Buy Located at " + add)
                            .createIntent();

                    twitterClicked = true;

                    startActivityForResult(intent, TWEET_COMPOSER_REQUEST_CODE);

                    shared = false;
                }

                /*twitterDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {;

                        for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                            if(dsp.getChildren().toString() == location){
                                shared = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                //Because it's against Facebook policy to prefill text in a post, a real website URL
                //Could be placed here with the post
                if(shared) {
                    //TweetComposer.Builder builder = new TweetComposer.Builder(MainActivity.this)
                    //        .text("I got my Smart Homes Thermostat at Best Buy Located at " + add);
                    //builder.show();
                    Intent intent = new TweetComposer.Builder(MainActivity.this)
                            .text("I got my Smart Homes Thermostat at Best Buy Located at " + add)
                            .createIntent();

                    twitterClicked = true;

                    startActivityForResult(intent, TWEET_COMPOSER_REQUEST_CODE);

                    shared = false;
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                    builder.setTitle("Already Posted")
                            .setMessage("You've already posted for this store")
                            .create().show();
                }*/

            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Batch.onStart(this);

    }

    @Override
    protected void onStop()
    {
        Batch.onStop(this);

        super.onStop();

    }

    @Override
    protected void onDestroy()
    {
        Batch.onDestroy(this);

        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        Batch.onNewIntent(this, intent);

        super.onNewIntent(intent);
    }

    //Method that initiates the Facebook button and wiring
    private void facebookShareButtonInitiate(){
        shareButton = (ImageButton) findViewById(R.id.facebook_button);
        shareButton.setVisibility(View.INVISIBLE);

        FacebookSdk.sdkInitialize(getApplicationContext());

        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                facebookDBRef.push().setValue(location);
                fbClicked = false;
                shared = false;
            }

            @Override
            public void onCancel() {
                fbClicked = false;
            }

            @Override
            public void onError(FacebookException error) {
                fbClicked = false;
            }
        });
    }


    private void twitterShareButtonInitiate() {
        twitterButton = (ImageButton) findViewById(R.id.twitter_button);
        twitterButton.setVisibility(View.INVISIBLE);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));


    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){

        savedInstanceState.putDouble("lat", latitude);
        savedInstanceState.putDouble("long", longitude);
        savedInstanceState.putString("add", add);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(fbClicked)
            callbackManager.onActivityResult(requestCode, resultCode, data);

        if(requestCode == TWEET_COMPOSER_REQUEST_CODE) {

            if(resultCode == RESULT_OK) {

                String location = "" + latitude + longitude;
                twitterDBRef.push().setValue(location);

            } else if(resultCode == RESULT_CANCELED) {


            }
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }

    //Dialog with effects for no stores found result
    public void alreadyPostedSocialMedia(String socialMediaType){
        //Instantiate the dialog effect
        alreadyPostedDialog = NiftyDialogBuilder.getInstance(this);
        alreadyPostedDialog.withTitle("Already Posted")
                .withMessage("You've already "+ socialMediaType +" that you've purchased a new Smart Homes Inc. " +
                        "thermostat at this location")
                .withDialogColor(android.R.color.holo_blue_light)
                .withButton1Text("OK").withEffect(Effectstype.Sidefill).setButton1Click(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alreadyPostedDialog.dismiss();
            }
        }).show();
    }

}
