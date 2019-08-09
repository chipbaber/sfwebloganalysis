package com.chip.code;

import java.util.ArrayList;
import java.util.List;

/*The SF Session Object holds the core session information from a SF log File.
 *
 */
public class sfSession {
    String accelerate_name, v_date, emailaddress, group, browser;

    List<sfAction> actions = new ArrayList<sfAction>();

    public sfSession() {

    }

    public void addAction(sfAction v_action){
        actions.add(v_action);
    }

    public void clearActions() {
        actions.clear();
    }

    public List getActions() {
        return actions;
    }

    public void setAccelerate_name(String accelerate_name) {
        this.accelerate_name = accelerate_name;
    }

    public String getAccelerate_name() {
        return accelerate_name;
    }

    public void setV_date(String v_date) {
        this.v_date = v_date;
    }

    public String getV_date() {
        return v_date;
    }

    public void setEmailaddress(String emailaddress) {
        this.emailaddress = emailaddress;
    }

    public String getEmailaddress() {
        return emailaddress;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getBrowser() {
        return browser;
    }
}
