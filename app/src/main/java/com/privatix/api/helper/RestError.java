package com.privatix.api.helper;

public class RestError {

//    @Expose
//    private List<Error> errors = new ArrayList<Error>();
//
//    public List<Error> getErrors() {
//        return errors;
//    }
//
//    public void setErrors(List<Error> errors) {
//        this.errors = errors;
//    }

    String message;

    int error_code;


    public String getMessage() {
        return message;
    }

    public int getError_code() {
        return error_code;
    }
}