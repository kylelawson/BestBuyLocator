package com.lawdogstudio.kyle.bestbuylocator;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Kyle on 4/29/2016.
 */
public class JSONFragment extends Fragment {

    public static final String bbyAPIKey = "8uhwa4p7xe34pqz6u3eqj3yq";

    String jsonResponse = "";
    String zipCode = "";
    String radius = "";
    String url;

    ArrayList<String> bbyArray = new ArrayList<>();

    ListView bbyList;

    //Progress dialog box
    ProgressDialog pd;

    ArrayAdapter<String> arrayAdapter;

    //Simple persistence with shared preference
    SharedPreferences sharedPref;

    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.main_screen_fragment, container, false);

        //Instantiate the listview
        bbyList = (ListView) view.findViewById(android.R.id.list);

        //Instantiate the adapter
        arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, bbyArray);

        //Attach the adapter to the listview
        bbyList.setAdapter(arrayAdapter);

        //Check for passed arguments from the bottom sheet
        checkForBundle();

        return view;
    }

    //JSON Object request method call from Volley Gradle import
    public void JSONAction(final String zipCodeIn, String radiusIn) {

        //Instantiation of progress dialog attributes
        pd = new ProgressDialog(getActivity());
        pd.setTitle("Getting Search Results");
        pd.setMessage("Please wait.");
        pd.setCancelable(false);

        //Set the url to be used given the zip code and radius
        url = "https://api.bestbuy.com/v1/stores(area(" + zipCodeIn + "," + radiusIn + "))" +
                "?format=json&show=storeType,name,address,city,region,postalCode,distance" +
                "&sort=distance.asc&apiKey=" + bbyAPIKey;

        //Create an object request object
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {

            //Override the response method of the object request, this is where the logic is
            @Override
            public void onResponse(JSONObject response) {

                try {
                    //Best Buy uses an array that holds their store information in a JSON object
                    //within the array so we need to pull the array, pull the stores object from
                    //the array, and store the individual store data in strings for display

                    //Calls the high level JSON Array that Best Buy sends
                    JSONArray stores = response.getJSONArray("stores");


                    //Use the parsing method with the array
                    parseJson(stores);


                    //After iteration is complete, the array adapter is notified that the store array
                    //data has been changed and updates the view accordingly
                    arrayAdapter.notifyDataSetChanged();

                    //Progress dialog dismissal must be placed after retrieval
                    pd.dismiss();

                } catch (JSONException e) {
                    Toast.makeText(getActivity(), "There was an error with the Volley result retrieval",
                            Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity(), "There was an error with the Volley request",
                        Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

        });

        //Call the network request
        Volley.newRequestQueue(getActivity()).add(jsonRequest);

        //Progress dialog must be placed after adding request to volley queue
        pd.show();

    }

    private void checkForBundle(){
        sharedPref = getActivity().getSharedPreferences("SEARCH_LIST", getActivity().MODE_PRIVATE);
        //If arguments have been passed by the bottom view via the activity, use them
        if(getArguments() != null) {
            radius = getArguments().getString("radius");
            zipCode = getArguments().getString("zip");

            //Instantiate a network status manager
            ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            //Instantiate a network info object and assign it the network info
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            //If network status connection return true, call the Best Buy API
            // If false, create a toast saying so
            if(isConnected)
            {

                //Call the network method
                JSONAction(zipCode, radius);

            }else{
                Toast.makeText(getActivity(), "No Data Connection", Toast.LENGTH_LONG).show();

                //Set the retry button to visible
                getActivity().findViewById(R.id.try_again_button).setVisibility(View.VISIBLE);
            }
        }else if(sharedPref.contains("Size")){ //Makes sure the app has data stored before retrieving

                retrieveArray();

                //Hides the initial FAB instruction textiview since there already is data
                getActivity().findViewById(R.id.initial_fb_text).setVisibility(View.GONE);
        }
    }

    //Method that stores the array in shared preferences
    private void storeArray(int i, String jsonResponse){

        //Instantiate the shared preferences variable object
        sharedPref = getActivity().getSharedPreferences("SEARCH_LIST", getActivity().MODE_PRIVATE);

        //Apply an editor
        SharedPreferences.Editor editor = sharedPref.edit();

        //Assign the position number to a string as the key and put the value into shared preferences
        String key = String.valueOf(i);
        editor.putString("" + key, jsonResponse);
        editor.commit();
    }

    //Retrieves the array from shared preferences
    private void retrieveArray() {
        sharedPref = getActivity().getSharedPreferences("SEARCH_LIST", getActivity().MODE_PRIVATE);

        //Get the size of the array
        int length = sharedPref.getInt("Size", 0);

        //Use the size of the array to add each key-value pair to the array for listview update in order
        //The key being the the position assigned from the storeArray method
        for(int i = 0; i < length; i++){
            bbyArray.add(sharedPref.getString(""+i, "None"));
        }

        arrayAdapter.notifyDataSetChanged();

    }

    private void parseJson(JSONArray stores) throws JSONException {

        //Instantiate the shared preferences variable object
        sharedPref = getActivity() .getSharedPreferences("SEARCH_LIST", getActivity().MODE_PRIVATE);

        //Add an editor
        SharedPreferences.Editor editor = sharedPref.edit();

        //Add the amount of stores returned into the shared preferences
        editor.putInt("Size", stores.length());
        editor.commit();

        //Variable setup
        String storeType = "";
        String storeName = "";
        String address = "";
        String city = "";
        String state = "";
        String zip = "";
        String distance = "";
        JSONObject storeObject;


        //Iterates through the array pulling each individual store's information,
        //which is put together as an object and assigns each key-value pair to a variable
        for (int i = 0; i < stores.length(); i++) {
            storeObject = stores.getJSONObject(i);
            storeType = storeObject.getString("storeType");
            storeName = storeObject.getString("name");
            address = storeObject.getString("address");
            city = storeObject.getString("city");
            state = storeObject.getString("region");
            zip = storeObject.getString("postalCode");
            distance = storeObject.getString("distance");


            //Places the variables into a sentence variable, use \r\n to make space between
            //Listview items, better practice is to make a custom view adapter but this
            //will work for now
            jsonResponse = "\r\n" + storeName + " - " + storeType +
                    "\r\nAddress: " + address + "\r\n" + city + ", " + state + ", "
                    + zip + " - " + distance + " miles away" + "\r\n";

            //Adds the sentence to the store array
            bbyArray.add(jsonResponse);

            //Add this sentence to the shared preferences, keeping it in order with this method
            storeArray(i, jsonResponse);

        }
    }

}
