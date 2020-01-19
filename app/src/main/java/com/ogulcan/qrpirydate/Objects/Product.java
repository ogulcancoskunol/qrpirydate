package com.ogulcan.qrpirydate.Objects;

import android.net.Uri;

import java.util.Date;

public class Product {
    private String name;
    private String img;
    private Date expiryDate, productionDate;

    public Product(String name, String img, Date productionDate, Date expiryDate){
        this.name = name;
        this.img = img;
        this.productionDate = productionDate;
        this.expiryDate = expiryDate;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public void setImg(String img){
        this.img = img;
    }

    public String getImg(){
        return img;
    }

    public void setProductionDate(Date productionDate){
        this.productionDate = productionDate;
    }

    public Date getProductionDate(){
        return productionDate;
    }

    public void setExpiryDate(Date expiryDate){
        this.expiryDate = expiryDate;
    }

    public Date getExpiryDate(){
        return expiryDate;
    }


}
