package com.adherence.adherence;

import android.app.DownloadManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;


import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MedicationFragment extends Fragment {

    private static final String ARG_MEDICINE_LIST = "medicine list";
    private static final String ARG_MEDICINE_DETAIL = "medicine detail";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_SESSION_TOKEN="session_token";

    private RecyclerView mRecyclerView;
    private MedicationListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private String[] medicineListHardcode;
    private String[] detailListHardcode;
    private String[] timeListHardcode;
    private String sessionToken;
    private String mDay;

    private Context mContext=null;

    private TextView pop_pillname;
    private TextView pop_pillinfo;
    private TextView pop_pillinstruction;

    private Prescription[] prescriptions;
    private static RequestQueue mRequestQueue;

    public Calendar c = Calendar.getInstance();

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MedicationFragment newInstance(String[] medicineList,String[] detailList, String sessionToken, int sectionNumber) {
        MedicationFragment fragment = new MedicationFragment();
        Bundle args = new Bundle();
        args.putStringArray(ARG_MEDICINE_LIST, medicineList);
        args.putStringArray(ARG_MEDICINE_DETAIL, detailList);
        args.putString(ARG_SESSION_TOKEN,sessionToken);
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        medicineListHardcode = getArguments().getStringArray(ARG_MEDICINE_LIST);
//        detailListHardcode = getArguments().getStringArray(ARG_MEDICINE_DETAIL);
        sessionToken=getArguments().getString(ARG_SESSION_TOKEN);
        Log.d("medi_fragment session",sessionToken);
        mContext = this.getContext();
        mRequestQueue = Volley.newRequestQueue(getActivity());
        Log.d("sequence", "onCreate!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_medication, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.medication_list);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
//            mRequestQueue= Volley.newRequestQueue(getActivity());

        String int_day=String.valueOf(c.get(c.DAY_OF_WEEK));
        switch (int_day){
            case "1":mDay="Sunday";break;
            case "2":mDay="Monday";break;
            case "3":mDay="Tuesday";break;
            case "4":mDay="Wednesday";break;
            case "5":mDay="Thursday";break;
            case "6":mDay="Friday";break;
            case "7":mDay="Saturday";break;
            default:mDay="Sunday";break;
        }

        String url= getString(R.string.parseURL) + "/patient/prescription";

        final JsonArrayRequest prescriptionRequest=new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                Log.d("response",response.toString());

                //retrive data from JSONobject
                int i = response.length();
                medicineListHardcode=new String[i];
                detailListHardcode=new String[i];
                timeListHardcode = new String[i];



                prescriptions = new Prescription[i];
                for (int j = 0;j < i;j++){
                    prescriptions[j] = new Prescription();
                    try {
                        JSONObject prescript = response.getJSONObject(j);
                        prescriptions[j].setName(prescript.getString("name"));
                        if(prescript.has("bottle")){
                            if(prescript.getJSONObject("bottle").has("bottleName")){
                            prescriptions[j].setBottleName(prescript.getJSONObject("bottle").getString("bottleName"));
                            }
                            if(prescript.getJSONObject("bottle").has("pillNumber")){
                            prescriptions[j].setPillNumber(prescript.getJSONObject("bottle").getInt("pillNumber"));
                            }
                        }else{
                            prescriptions[j].setBottleName("null");
                            prescriptions[j].setPillNumber(0);
                        }
                        if(prescript.has("note")) {
                            prescriptions[j].setNote(prescript.getString("note"));
                        }else{
                            prescriptions[j].setNote("none");
                        }
                        if(prescript.has("pill")) {
                            prescriptions[j].setPill(prescript.getString("pill"));
                        }else{
                            prescriptions[j].setPill("none");
                        }
                        if(prescript.has("newAdded")){
                            prescriptions[j].setNewAdded(prescript.getBoolean("newAdded"));
                        }else {
                            prescriptions[j].setNewAdded(false);
                        }
                        //prescriptions[j].setPrescriptionId(prescript.getString("objectId"));

                        JSONArray schedule = prescript.getJSONArray("schedule");


                        for(int k = 0; k < schedule.length(); k++){
                            JSONObject takeTime = schedule.getJSONObject(k);
                            String time = takeTime.getString("time").substring(11, 19);
                            JSONArray takeWeek = takeTime.getJSONArray("days");
                            Map<String, Integer> days = new HashMap<String, Integer>();
                            for(int l = 0; l < takeWeek.length(); l++){
                                JSONObject takeDays = takeWeek.getJSONObject(l);
                                if(takeDays.has("amount")){
                                    days.put(takeDays.getString("name"), takeDays.getInt("amount"));
                                }else {
                                    days.put(takeDays.getString("name"), 0);
                                }
                            }
                            prescriptions[j].setSchedule(time, days);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                // test if data stored in prescriptions

                for(int j = 0; j < i; j++) {

                    System.out.println(prescriptions[j].getName());
                    medicineListHardcode[j]=prescriptions[j].getName();
                    System.out.println(prescriptions[j].getNote());
                    detailListHardcode[j]=prescriptions[j].getNote();

                    System.out.println(prescriptions[j].getBottleName());
                    System.out.println(prescriptions[j].getNewAdded());
                    System.out.println(prescriptions[j].getPillNumber());



                    Iterator<Map.Entry<String, Integer>> itr = prescriptions[j].getTimeAmount("Monday").entrySet().iterator();
                    while(itr.hasNext()){
                        Map.Entry<String, Integer> entry = itr.next();
                        int cur_hour = Integer.parseInt(entry.getKey().substring(0, 2));



                        if (7 <= cur_hour && cur_hour < 12) {
                            if (timeListHardcode[j] == null) {
                                timeListHardcode[j] = "Morning" + "\n";
                            }else {
                                timeListHardcode[j] += "Morning" + "\n";
                            }
                        }

                        if (12 <= cur_hour && cur_hour < 18) {
                            if (timeListHardcode[j] == null) {
                                timeListHardcode[j] = "Afternoon" + "\n";
                            }else {
                                timeListHardcode[j] += "Afternoon" + "\n";
                            }
                        }

                        if (18 <= cur_hour && cur_hour <= 24) {
                            if (timeListHardcode[j] == null) {
                                timeListHardcode[j] = "Evening" + "\n";
                            }else {
                                timeListHardcode[j] += "Evening" + "\n";
                            }
                        }

                        if (0 <= cur_hour && cur_hour < 7) {
                            if (timeListHardcode[j] == null) {
                                timeListHardcode[j] = "Bedtime" + "\n";
                            }else {
                                timeListHardcode[j] += "Bedtime" + "\n";
                            }
                        }

                        timeListHardcode[j].trim();
                        Log.d("row", Integer.toString(j));
                        Log.d("time string", timeListHardcode[j]);

//                        Log.d("medic day", entry.getKey());
//                        Log.d("medic amount", entry.getValue().toString());
                    }
//                    Log.d("test time", timeListHardcode);



                    //traverse with Map.Entry
                    Iterator<Map.Entry<String, Map<String, Integer>>> it = prescriptions[j].getSchedule().entrySet().iterator();

                    while (it.hasNext()) {

                        // entry.getKey() return key
                        // entry.getValue() return value
                        Map.Entry<String, Map<String, Integer>> entry = (Map.Entry) it.next();
                        System.out.println(entry.getKey());

                        HashMap<String, Integer> tmp_in_hashmap = (HashMap) entry.getValue();

                        Iterator<Map.Entry<String, Integer>> in_iterator = tmp_in_hashmap
                                .entrySet().iterator();

                        while (in_iterator.hasNext()) {
                            Map.Entry in_entry = (Map.Entry) in_iterator.next();
                            System.out.println(in_entry.getKey() + ":" + in_entry.getValue());
                        }
                    }
                }


                mAdapter = new MedicationListAdapter(medicineListHardcode,detailListHardcode, timeListHardcode);
                mRecyclerView.setAdapter(mAdapter);


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("error",error.toString());
            }
        }){
            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("x-parse-session-token",sessionToken);
                return headers;
            }
        };


        //prescriptions[0].requestSetBottleName(sessionToken, mRequestQueue);

        mRequestQueue.add(prescriptionRequest);


        Log.d("sequence","onCreateView!");


        //prescriptions[0].requestSetBottleName(sessionToken, mRequestQueue);
        //mRequestQueue.add(prescriptions[0].requestSetBottleName(sessionToken, prescriptions[0]));
       // System.out.println(prescriptions[0].getBottleName());


     /*   ParseQuery<ParseObject> query2=ParseQuery.getQuery("Prescription");
        query2.whereNotEqualTo("pill",null);
        query2.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(objects==null){
                    Toast.makeText(getActivity(),"objects is null!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i("objects.size another:",objects.size()+"");
                medicineListHardcode=new String[objects.size()];
                detailListHardcode=new String[objects.size()];
                Log.i("medicineListHardcode", medicineListHardcode.length+"");
                Log.i("detailListHardcode",detailListHardcode.length+"");
                for(int i=0;i<objects.size();i++){
                    medicineListHardcode[i]=objects.get(i).getString("name");
                    detailListHardcode[i]=objects.get(i).getString("note");
                }
                mAdapter = new MedicationListAdapter(medicineListHardcode,detailListHardcode);
                mRecyclerView.setAdapter(mAdapter);
                mAdapter.setOnItemClickListener(new MedicationListAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(final View view, int position) {
                        //Toast.makeText(getActivity(),position+"", Toast.LENGTH_SHORT).show();
                        ParseQuery<ParseObject>query3= ParseQuery.getQuery("Prescription");
                        query3.whereEqualTo("name",medicineListHardcode[position]);
                        query3.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> objects, ParseException e) {
                                String pillID=objects.get(0).getParseObject("pill").getObjectId();
                                ParseQuery<ParseObject>query4=ParseQuery.getQuery("PillLib");
                                query4.whereEqualTo("objectId",pillID);
                                query4.findInBackground(new FindCallback<ParseObject>() {
                                    @Override
                                    public void done(List<ParseObject> objects, ParseException e) {
                                        String pillname=objects.get(0).getString("pillName");
                                        String pillinfo=objects.get(0).getString("pillInfo");
                                        String pillinstruction=objects.get(0).getString("pillInstruction");
                                        showPopupWindow(view,"pillName: "+pillname,"pillInfo: "+pillinfo,"pillInstruction: "+pillinstruction);
                                    }
                                });
                            }
                        });
                        //   showPopupWindow(view);
                    }
                    @Override
                    public void onItemLongClick(final View view, int position) {
                        ParseQuery<ParseObject>query3= ParseQuery.getQuery("Prescription");
                        query3.whereEqualTo("name",medicineListHardcode[position]);
                        query3.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> objects, ParseException e) {
                                String pillID=objects.get(0).getParseObject("pill").getObjectId();
                                ParseQuery<ParseObject>query4=ParseQuery.getQuery("PillLib");
                                query4.whereEqualTo("objectId",pillID);
                                query4.findInBackground(new FindCallback<ParseObject>() {
                                    @Override
                                    public void done(List<ParseObject> objects, ParseException e) {
                                        String pillname=objects.get(0).getString("pillName");
                                        String pillinfo=objects.get(0).getString("pillInfo");
                                        String pillinstruction=objects.get(0).getString("pillInstruction");
                                        showPopupWindow(view,"pillName: "+pillname,"pillInfo: "+pillinfo,"pillInstruction: "+pillinstruction);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });*/
//        mAdapter = new MedicationListAdapter(detailListHardcode,detailListHardcode);
//        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NextActivity){
            ((NextActivity) context).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    private void showPopupWindow(View view,String pillname,String pillinfo,String pillinstruction) {


        View contentView = LayoutInflater.from(mContext).inflate(
                R.layout.pop_up_window, null);


        final PopupWindow popupWindow = new PopupWindow(contentView,
                RecyclerView.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, true);

        pop_pillname= (TextView) contentView.findViewById(R.id.pop_pillname);
        pop_pillinfo= (TextView) contentView.findViewById(R.id.pop_pillinfo);
        pop_pillinstruction= (TextView) contentView.findViewById(R.id.pop_instruction);
        pop_pillname.setText(pillname);
        pop_pillinfo.setText(pillinfo);
        pop_pillinstruction.setText(pillinstruction);
        popupWindow.setTouchable(true);

        popupWindow.setTouchInterceptor(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Log.i("mengdd", "onTouch : ");

                return false;

            }
        });


        popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape));
//        popupWindow.setBackgroundDrawable(null);


        View windowContentViewRoot = getView();
        int windowPos[] = calculatePopWindowPos(view, windowContentViewRoot);
        int xOff = 20;
        windowPos[0] -= xOff;
        popupWindow.showAtLocation(view, Gravity.TOP | Gravity.START, windowPos[0], windowPos[1]);


    }

    private static int[] calculatePopWindowPos(final View anchorView, final View contentView) {
        final int windowPos[] = new int[2];
        final int anchorLoc[] = new int[2];

        anchorView.getLocationOnScreen(anchorLoc);
        final int anchorHeight = anchorView.getHeight();

        final int screenHeight = ScreenUtils.getScreenHeight(anchorView.getContext());
        final int screenWidth = ScreenUtils.getScreenWidth(anchorView.getContext());
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        final int windowHeight = contentView.getMeasuredHeight();
        final int windowWidth = contentView.getMeasuredWidth();

        final boolean isNeedShowUp = (screenHeight - anchorLoc[1] - anchorHeight < windowHeight);
        if (isNeedShowUp) {
            windowPos[0] = screenWidth - windowWidth;
            windowPos[1] = anchorLoc[1] - windowHeight;
        } else {
            windowPos[0] = screenWidth - windowWidth;
            windowPos[1] = anchorLoc[1] + anchorHeight;
        }
        return windowPos;
    }

//    public void onClick(View v) {
//        int temp =  0;
//    }
}
