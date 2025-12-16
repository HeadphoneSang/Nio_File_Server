package org.template.server.components.pojo;

public class Future {

    public void setSuccess(boolean success) {
        this.success = success;
    }

    private boolean success;

    public boolean isSuccess(){
        return success;
    }

    public boolean isNotSuccess(){
        return !success;
    }

}
