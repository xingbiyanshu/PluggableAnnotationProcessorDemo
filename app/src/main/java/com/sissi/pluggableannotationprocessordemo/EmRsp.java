package com.sissi.pluggableannotationprocessordemo;


import com.sissi.annotation.RspClazz;
import com.sissi.annotation.RspDef;

/**
 * Created by Sissi on 2018/9/3.
 */

@RspDef
public enum EmRsp {
    @RspClazz(String.class)
    LoginRsp,
    @RspClazz(String.class)
    LoginRspFin,
    @RspClazz(Enum.class)
    LogoutRsp,
    @RspClazz(Enum.class)
    LogoutRspFin,
}
