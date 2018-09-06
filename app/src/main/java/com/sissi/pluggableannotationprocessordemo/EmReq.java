package com.sissi.pluggableannotationprocessordemo;


import com.sissi.annotation.ReqDef;
import com.sissi.annotation.Request;

/**
 * Created by Sissi on 2018/9/3.
 */

@ReqDef(rspDefClass = EmRsp.class)
public enum EmReq {

    @Request(reqPara = ReqRspBeans.Login.class, rspSeq = {"LoginRsp", "LoginRspFin"}, timeout = 6)
    LoginReq,

    @Request(reqPara = ReqRspBeans.Logout.class, rspSeq = {"LogoutRsp", "LogoutRspFin"}, timeout = 5)
    LogoutReq,

}
