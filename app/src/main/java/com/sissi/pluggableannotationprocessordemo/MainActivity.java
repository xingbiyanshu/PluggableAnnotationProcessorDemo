package com.sissi.pluggableannotationprocessordemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Map<String, Integer> timeoutMap = DmMsg$$MessageProcessor.reqTimeoutMap;
        for (String key : timeoutMap.keySet()){
//            Log.i("", "key="+key+" timeout="+timeoutMap.get(key));
            PcTrace.p("key="+key+" timeout="+timeoutMap.get(key));
        }

        Set<Class> serializeEnumAsIntSet = SerializeEnumAsInt$$SerializationProcessor.serializeEnumAsIntSet;
        for (Class clz : serializeEnumAsIntSet){
            PcTrace.p("clz="+clz);
        }
    }
}
