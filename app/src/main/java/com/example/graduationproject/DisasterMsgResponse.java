package com.example.graduationproject;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "DisasterMsg2", strict = false)
public class DisasterMsgResponse {

    @ElementList(entry = "row", inline = true, required = false)
    private List<DisasterMsg> row;

    // row 리스트를 반환하는 getter 메소드
    public List<DisasterMsg> getRow() {
        return row;
    }

    @Root(name = "row", strict = false)
    public static class DisasterMsg {

        @Element(name = "create_date", required = false)
        private String createDate;

        @Element(name = "location_id", required = false)
        private String locationId;

        @Element(name = "location_name", required = false)
        private String locationName;

        @Element(name = "msg", required = false)
        private String msg;

        @Element(name = "md101_sn", required = false)
        private String md101Sn;

        // Getters
        public String getCreateDate() {
            return createDate;
        }

        public String getLocationId() {
            return locationId;
        }

        public String getLocationName() {
            return locationName;
        }

        public String getMsg() {
            return msg;
        }

        public String getMd101Sn() {
            return md101Sn;
        }
    }
}
