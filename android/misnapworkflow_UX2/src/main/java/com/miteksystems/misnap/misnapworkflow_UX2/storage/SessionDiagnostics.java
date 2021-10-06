package com.miteksystems.misnap.misnapworkflow_UX2.storage;


import com.miteksystems.misnap.analyzer.MiSnapAnalyzerResult;
import com.miteksystems.misnap.params.DocType;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Created by jlynch on 8/26/16.
 */
public class SessionDiagnostics {
    public static final String FAILURE_TYPE = "FAILURE_TYPE";
    public static final String FAILURE_PERCENT = "FAILURE_PERCENT";
    private static final int MINIMUM_ERROR_THRESHOLD = 26; // 26 x 4 > 100, so a max of 3 help reasons can be displayed
    private static final int REJECT_MODULATOR = 2;

    private DocType docType;
    private HashMap<MiSnapAnalyzerResult.FrameChecks, Integer> failures;
    private ArrayList<MiSnapAnalyzerResult.FrameChecks> fieldList;
    private int lowConfidenceBrightnessFailCount = 0;
    private int angleBrightnessFailCount = 0;
    private int frameCount = 0;

    public SessionDiagnostics(DocType docType) {
        this.docType = docType;
        failures = new HashMap<>();
        fieldList = new ArrayList<>();
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.MIN_BRIGHTNESS);
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.BUSY_BACKGROUND);
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.LOW_CONTRAST);
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.SHARPNESS);
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.HORIZONTAL_MINFILL);
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.MIN_PADDING);
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE);
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.ROTATION_ANGLE);
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.MAX_SKEW_ANGLE);
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.WRONG_DOCUMENT);
        fieldList.add(MiSnapAnalyzerResult.FrameChecks.GLARE);

        reset();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    public void reset(){
        lowConfidenceBrightnessFailCount = 0;
        angleBrightnessFailCount = 0;
        failures.clear();

        for(MiSnapAnalyzerResult.FrameChecks field:fieldList){
            failures.put(field, 0);
        }
    }

    public void deInit(){
        EventBus.getDefault().unregister(this);
    }

    public ArrayList<String> rankFailures(){
        LinkedHashMap<MiSnapAnalyzerResult.FrameChecks, Integer> rank = new LinkedHashMap<>();
        ArrayList<String> result = new ArrayList<>();
        int failCount = 0;

        for(MiSnapAnalyzerResult.FrameChecks tag: fieldList){
            failCount += failures.get(tag);
        }

        for(MiSnapAnalyzerResult.FrameChecks tag: fieldList){
            rank.put(tag, failures.get(tag));
        }
        rank = sortByComparator(rank);

        for (Map.Entry<MiSnapAnalyzerResult.FrameChecks, Integer> entry : rank.entrySet()){
            if(entry != null && getPercentage(entry.getValue(), failCount) >= MINIMUM_ERROR_THRESHOLD
                    && getPercentage(entry.getValue(), frameCount) >= MINIMUM_ERROR_THRESHOLD){
                JSONObject holder = new JSONObject();
                try {
                    holder.put(FAILURE_TYPE, entry.getKey());
                    holder.put(FAILURE_PERCENT, getPercentage(entry.getValue(), failCount));

                    if (entry.getKey().equals(MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE)) {
                        if (docType.isCheckFront()) {
                            JSONObject extraMessage = getMessageEnsureNumbersAreVisible();
                            result.add(extraMessage.toString());
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                result.add(holder.toString());
            }
        }
        // If no failover reason stood out, return a default one
        if (result.size() == 0) {
            JSONObject holder = new JSONObject();
            try {
                holder.put(FAILURE_TYPE, MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE);
                holder.put(FAILURE_PERCENT, 100);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            result.add(holder.toString());

            if (docType.isCheckFront()) {
                JSONObject extraMessage = getMessageEnsureNumbersAreVisible();
                result.add(extraMessage.toString());
            }
        }

        return result;
    }

    private JSONObject getMessageEnsureNumbersAreVisible() {
        JSONObject holder = new JSONObject();
        if (docType.isCheckFront()) {
            try {
                holder.put(FAILURE_TYPE, MiSnapAnalyzerResult.FrameChecks.WRONG_DOCUMENT);
                holder.put(FAILURE_PERCENT, 100);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return holder;
    }

    private LinkedHashMap<MiSnapAnalyzerResult.FrameChecks, Integer> sortByComparator(LinkedHashMap<MiSnapAnalyzerResult.FrameChecks, Integer> enteredData)
    {

        List<Map.Entry<MiSnapAnalyzerResult.FrameChecks, Integer>> sortedData = new LinkedList<>(enteredData.entrySet());

        Collections.sort(sortedData, new Comparator<Map.Entry<MiSnapAnalyzerResult.FrameChecks, Integer>>()
        {
            public int compare(Map.Entry<MiSnapAnalyzerResult.FrameChecks, Integer> entryA,
                               Map.Entry<MiSnapAnalyzerResult.FrameChecks, Integer> entryB)
            {
                return entryB.getValue().compareTo(entryA.getValue());
            }
        });

        LinkedHashMap<MiSnapAnalyzerResult.FrameChecks, Integer> returnedData = new LinkedHashMap<>();
        for (Map.Entry<MiSnapAnalyzerResult.FrameChecks, Integer> entry : sortedData)
        {
            returnedData.put(entry.getKey(), entry.getValue());
        }

        return returnedData;
    }

    @Subscribe
    public void onEvent(MiSnapAnalyzerResult event) {
        ++frameCount;
        if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.GLARE)){
            incrementFailure(MiSnapAnalyzerResult.FrameChecks.GLARE);
        }
        if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.BUSY_BACKGROUND)){
            incrementFailure(MiSnapAnalyzerResult.FrameChecks.BUSY_BACKGROUND);
        }
        if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.LOW_CONTRAST)){
            incrementFailure(MiSnapAnalyzerResult.FrameChecks.LOW_CONTRAST);
        }

        if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.WRONG_DOCUMENT)){
            int weight = 1; // B-03473 - handle invalid doc type in auto mode
            if(docType.isCheckBack()) {
                weight = frameCount;
            }else if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.BUSY_BACKGROUND) ||
                    !event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.LOW_CONTRAST)){
                weight = 0;
            }
            incrementFailureByWeight(MiSnapAnalyzerResult.FrameChecks.WRONG_DOCUMENT, weight);
        }

        boolean minBrightnessFailed = !event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.MIN_BRIGHTNESS);
        boolean maxBrightnessFailed = !event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.MAX_BRIGHTNESS);
        if (minBrightnessFailed || maxBrightnessFailed) {
            MiSnapAnalyzerResult.FrameChecks brightnessCheckThatFailed = minBrightnessFailed ? MiSnapAnalyzerResult.FrameChecks.MIN_BRIGHTNESS : MiSnapAnalyzerResult.FrameChecks.MAX_BRIGHTNESS;
            //if four corners failed, only take half the errors
            if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE) && ++lowConfidenceBrightnessFailCount % REJECT_MODULATOR == 0){
                incrementFailure(brightnessCheckThatFailed);
            }else if(event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE)){
                //rotation still brings in lots of noisy background, ignore half of those brightness errors
                if(((!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.ROTATION_ANGLE) ||
                        !event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.MAX_SKEW_ANGLE)) && ++angleBrightnessFailCount % REJECT_MODULATOR == 0)){
                    incrementFailure(brightnessCheckThatFailed);
                }else if(event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.ROTATION_ANGLE) &&
                        event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.MAX_SKEW_ANGLE)){
                    incrementFailure(brightnessCheckThatFailed);
                }
            }
        }

        //following metrics depend on accurate four corners
        if(event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE)){
            if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.SHARPNESS)){
                incrementFailure(MiSnapAnalyzerResult.FrameChecks.SHARPNESS);
            }
            if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.ROTATION_ANGLE) ||
                    !event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.MAX_SKEW_ANGLE)){
                if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.MAX_SKEW_ANGLE)){
                    incrementFailure(MiSnapAnalyzerResult.FrameChecks.MAX_SKEW_ANGLE);
                }
                if (!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.ROTATION_ANGLE)){
                    incrementFailure(MiSnapAnalyzerResult.FrameChecks.ROTATION_ANGLE);
                }
            }else{
                //high angles can cause these issues, but they are symptoms rather than causes
                if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.MIN_PADDING)){
                    incrementFailure(MiSnapAnalyzerResult.FrameChecks.MIN_PADDING);
                }
                if(!event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.HORIZONTAL_MINFILL)){
                    incrementFailure(MiSnapAnalyzerResult.FrameChecks.HORIZONTAL_MINFILL);
                }
            }
        }
    }

    private int getPercentage(int numerator, int denominator){
        if(denominator == 0){
            return 0;
        }
        return (int)(((float)numerator/denominator)*100);
    }

    private void incrementFailure(MiSnapAnalyzerResult.FrameChecks tag){
        failures.put(tag, failures.get(tag) + 1);
    }

    private void incrementFailureByWeight(MiSnapAnalyzerResult.FrameChecks tag, int amount) {
        failures.put(tag, failures.get(tag) + amount);
    }
}
