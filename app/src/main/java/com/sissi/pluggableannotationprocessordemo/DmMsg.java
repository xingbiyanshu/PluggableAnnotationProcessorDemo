package com.sissi.pluggableannotationprocessordemo;


import com.sissi.annotation.Message;
import com.sissi.annotation.Request;
import com.sissi.annotation.Response;

/**
 * Created by Sissi on 2018/9/3.
 */

@Message
public enum DmMsg { // Domain Message

    // login
    @Request(reqPara = MsgBeans.Login.class, rspSeq = {"LoginRsp", "LoginRspFin"}, timeout = 6)
    LoginReq,
    @Response(MsgBeans.LoginRsp.class)
    LoginRsp,
    @Response(String.class)
    LoginRspFin,

    // logout
    @Request(reqPara = MsgBeans.Logout.class, rspSeq = {"LogoutRsp", "LogoutRspFin"}, timeout = 5)
    LogoutReq,
    @Response(Enum.class)
    LogoutRsp,
    @Response(Enum.class)
    LogoutRspFin,

}
