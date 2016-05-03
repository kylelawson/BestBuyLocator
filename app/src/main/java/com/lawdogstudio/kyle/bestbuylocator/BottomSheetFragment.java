package com.lawdogstudio.kyle.bestbuylocator;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Created by Kyle on 4/22/2016.
 */
public class BottomSheetFragment extends BottomSheetDialogFragment{

    //Set all global variables
    EditText zipCodeBox;
    TextView radiusTitleBox;
    SeekBar radiusSeekBar;
    Button searchButton;
    String radius = "", zipCode = "";
    View view;
    onSetListener onSetListener;

    //Boolean variables used in the button logic, used to show only the necessary toasts
    boolean seekBarBoolean = false, zipcodeBoolean = false;

    //Overriding the bottomsheet behavior methods
    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {

            //Used to dismiss the fragment from memory if the bottomsheet is hidden, releases memory
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }

        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    //Sets up the the bottomsheet dialog fragment
    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.search_bottom_sheet, null);
        dialog.setContentView(contentView);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        //Sets up bottomsheet call back
        if( behavior != null && behavior instanceof BottomSheetBehavior ) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }

    }

    //Where the bottomsheet fragment logic is
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.search_bottom_sheet, container, false);

        //Initialize the zip code field
        zipCodeBox = (EditText) view.findViewById(R.id.zipcode_textbox);

        //Initialize the box above the seekbar
        radiusTitleBox = (TextView) view.findViewById(R.id.search_radius_textbox);

        //Initialize the seekbar
        radiusSeekBar = (SeekBar) view.findViewById(R.id.search_radius_seekBar);

        //Attach action listener to seekbar and override the methods
        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Sets the seekbar title to the mileage indicated by the seekbar movement
                radiusTitleBox.setText(String.valueOf(progress) + " Mile Radius");

                //Store the value of the seekbar everytime it changes
                radius = String.valueOf(progress);

                //Set the seekbar boolean for future logic use
                seekBarBoolean = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Initialize the search button
        searchButton = (Button) view.findViewById(R.id.search_button);

        //Set the button listener
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Parse the text from the zip code box into an int then return a boolean
                zipcodeBoolean = parseTheZipcode();

                //Boolean values are used here to determine which toast is used
                //This prevents multiple toasts from appearing which can get annoying
                if(seekBarBoolean == true && zipcodeBoolean == true) {

                    //Use the interface to pass the zip code and radius input to the activity
                    onSetListener.setSearchCriteria(zipCode, radius);

                    //Get rid of the fragment upon success
                    dismiss();

                }else if(seekBarBoolean == true && zipcodeBoolean == false){

                }else if(seekBarBoolean == false & zipcodeBoolean == true){
                    Toast.makeText(getActivity(), "Please Pick a Search Radius", Toast.LENGTH_SHORT).show();
                }else if(seekBarBoolean == false && zipcodeBoolean == false){
                    Toast.makeText(getActivity(), "Please Pick a Search Radius", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    //Private method that determines whether the zip code box has valid input in it or not
    private boolean parseTheZipcode(){
        if(zipCodeBox != null && zipCodeBox.length() == 5){

            zipCode = zipCodeBox.getText().toString();
            return true;

        }else{

            Toast.makeText(getActivity(), "Please Enter a 5 digit Zip Code", Toast.LENGTH_SHORT).show();
            return false;

        }
    }

    //Custom interface used to pass data to the activity
    public interface onSetListener
    {
        public void setSearchCriteria(String zipCodeIn, String radiusIn);
    }

    //Makes sure the interface is implemented on this fragment's attachment to the activity
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            onSetListener = (onSetListener) activity;
        } catch (Exception e){

        }
    }
}
