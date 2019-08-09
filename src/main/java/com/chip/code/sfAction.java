package com.chip.code;

/*The SF Action Object holds the core session actions a SF log File.
 * A sfAction object must reside withing a sfSession Object.
 */

import java.util.Date;

public class sfAction {

    private Date v_actionTime;
    private String action;
    private String link;

    public void setV_actionTime(Date v_actionTime) {
        this.v_actionTime = v_actionTime;
    }

    public Date getV_actionTime() {
        return v_actionTime;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public sfAction() {
        super();
    }

}

