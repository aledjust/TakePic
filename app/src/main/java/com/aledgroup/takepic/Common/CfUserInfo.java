package com.aledgroup.takepic.Common;

/**
 * Created by aled on 05/03/2016.
 */
public class CfUserInfo {

    //private variables
    String rowId,loginCode,userName,userAlias,templateUser,imageName, settings;

    public CfUserInfo() {
    }

    public CfUserInfo(String rowId, String loginCode, String userName, String userAlias, String templateUser, String imageName) {
        this.rowId = rowId;
        this.loginCode = loginCode;
        this.userName = userName;
        this.userAlias = userAlias;
        this.templateUser = templateUser;
        this.imageName = imageName;
    }

    public CfUserInfo(String rowId, String templateUser) {
        this.rowId = rowId;
        this.templateUser = templateUser;
    }

    public String getRowId() {
        return rowId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }

    public String getLoginCode() {
        return loginCode;
    }

    public void setLoginCode(String loginCode) {
        this.loginCode = loginCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAlias() {
        return userAlias;
    }

    public void setUserAlias(String userAlias) {
        this.userAlias = userAlias;
    }

    public String getTemplateUser() {
        return templateUser;
    }

    public void setTemplateUser(String templateUser) {
        this.templateUser = templateUser;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }
}
