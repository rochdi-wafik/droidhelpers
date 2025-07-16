package com.iorgana.droidhelpers_implements.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class UserModel implements Serializable {
    public static final String DB_KEY = "db_key_UserModel";

    String fullname;
    Integer age;
    Boolean isAdmin;

    /**
     * Constructor
     * ------------------------------------------------------
     */
    public UserModel(String fullname, Integer age, Boolean isAdmin) {
        this.fullname = fullname;
        this.age = age;
        this.isAdmin = isAdmin;
    }

    /**
     * Getters
     */
    @NonNull
    public String getFullname() {
        return fullname;
    }

    @NonNull
    public Integer getAge() {
        return age;
    }

    @NonNull
    public Boolean isAdmin() {
        return isAdmin;
    }

    /**
     * Setters
     */
    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }

    /**
     * To String
     */
    @NonNull
    @Override
    public String toString() {
        return "UserModel{" +
                "fullname='" + fullname + '\'' +
                ", age=" + age +
                ", isAdmin=" + isAdmin +
                '}';
    }
}
