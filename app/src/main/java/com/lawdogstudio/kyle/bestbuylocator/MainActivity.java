package com.lawdogstudio.kyle.bestbuylocator;

import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

//Implements the bottom sheet's interface so that something can be done with the data received
public class MainActivity extends AppCompatActivity implements BottomSheetFragment.onSetListener {

    //The floating action button and instruction text
    FloatingActionButton fab;
    TextView initialTv;

    //Set a fragment manager for the main screen fragment
    FragmentManager fm = getSupportFragmentManager();
    Fragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Initiate the fragment itself by attaching it to the fragment manager and assigning it the
        //layout within the activity_main.xml
        mainFragment = fm.findFragmentById(R.id.list_fragment_container);

        //If the fragment is empty, attach a fragment activity class to it and begin a fragment transaction
        //in order to show the fragment in the main view
        if(mainFragment == null){
            mainFragment = new JSONFragment();
            fm.beginTransaction().add(R.id.list_fragment_container, mainFragment).commit();
        }

        initialTv = (TextView) findViewById(R.id.initial_fb_text);

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

    }

    //Override the implemented interface so that the data can be sent to the JSON fragment
    @Override
    public void setSearchCriteria(String zipCodeIn, String radiusIn) {

        //Uses bundle to send data
        Bundle bundle = new Bundle();
        bundle.putString("zip", zipCodeIn);
        bundle.putString("radius", radiusIn);

        //Instantiate a new JSONFragment that holds the bundle
        Fragment replacement = new JSONFragment();
        replacement.setArguments(bundle);

        //Use the fragment manager to replace the current JSON Fragment with the new one that has arguments
        fm.beginTransaction().replace(R.id.list_fragment_container, replacement).commit();

    }

}
