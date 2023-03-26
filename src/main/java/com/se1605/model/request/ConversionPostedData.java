package com.se1605.model.request;

public class ConversionPostedData extends PostedDataEntity {

    /**
     * path or url for first file
     */
    private String destinationType;

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }
}
