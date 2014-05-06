package com.xstd.ip.module;

import net.tsz.afinal.annotation.sqlite.NotNull;
import net.tsz.afinal.annotation.sqlite.Table;
import net.tsz.afinal.annotation.sqlite.Unique;

import java.io.Serializable;

/**
 * Created by chrain on 14-4-24.
 */
@Table(name = "Table_PushMessage")
public class PushMessage implements Serializable {
    private int _id;
    private String packageName;
    private int type;
    @Unique
    private String token;
    private boolean successful;

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
