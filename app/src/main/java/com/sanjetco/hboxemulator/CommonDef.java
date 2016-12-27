package com.sanjetco.hboxemulator;

/**
 * Created by PaulLee on 12/15/2016.
 * Common definition
 */

public interface CommonDef {
    String DEBUGKW = "HBOXEMU";

    int HB_CMD_IMG_REQ = 0x01;
    int HB_CMD_START = 0x02;
    int HB_CMD_IMG_DATA = 0x03;
    int HB_CMD_CONN = 0x04;

    class HoribaHeader {
        short cmd;
        short data_length;
    }

    class HoribaReply {
        HoribaHeader header;
        short status;
    }
}
