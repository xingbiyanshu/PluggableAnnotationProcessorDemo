package com.sissi.pluggableannotationprocessordemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


import java.util.Map;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        GenFile genFile = MainActivity.class.getAnnotation(GenFile.class);
//        Class.forName(genFile.packageName()+"."+genFile.className());

        Map<String, Integer> timeoutMap = DmMsg$$Generated.reqTimeoutMap;
        for (String key : timeoutMap.keySet()){
//            Log.i("", "key="+key+" timeout="+timeoutMap.get(key));
            PcTrace.p("key="+key+" timeout="+timeoutMap.get(key));
        }
//        Log.i("", ReqRspMap$$FromAnnotation.getReqRspsMap());
    }
}
