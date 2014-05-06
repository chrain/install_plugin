package com.xstd.ip.module;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.NotNull;
import net.tsz.afinal.annotation.sqlite.Table;
import net.tsz.afinal.annotation.sqlite.Unique;

import java.io.Serializable;

/**
 * Created by chrain on 14-4-22.
 */
@Table(name = "Table_ApplicationInfo")
public class ApplicationInfo implements Serializable {

    @Id
    private int _id;

    /**
     * 下载时候存的文件名
     */
    private String fileName;

    /**
     * 包名
     */
    @NotNull(true)
    @Unique(true)
    private String packageName;

    /**
     * 下载地址
     */
    @NotNull(true)
    private String downloadPath;

    /**
     * 存储在本地的地址
     */
    private String localPath;

    /**
     * 是否需要静默安装
     */
    private boolean silence;

    /**
     * 是否需要下载
     */
    private boolean download;

    /**
     * 是否安装
     */
    private boolean install;

    /**
     * 程序安装事件
     */
    private long installTime;

    /**
     * 程序卸载事件
     */
    private long uninstallTime;

    private String tickerText;
    private String title;
    private String text;
    private String token;

    public ApplicationInfo() {
    }

    public ApplicationInfo(String fileName, String packageName, String downloadPath, boolean silence, String token) {
        this.fileName = fileName;
        this.packageName = packageName;
        this.downloadPath = downloadPath;
        this.silence = silence;
        this.token = token;
    }

    public ApplicationInfo(String fileName, String packageName, String downloadPath, boolean silence, String tickerText, String title, String text) {
        this.fileName = fileName;
        this.packageName = packageName;
        this.downloadPath = downloadPath;
        this.silence = silence;
        this.tickerText = tickerText;
        this.title = title;
        this.text = text;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public boolean isSilence() {
        return silence;
    }

    public void setSilence(boolean silence) {
        this.silence = silence;
    }

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    public boolean isInstall() {
        return install;
    }

    public void setInstall(boolean install) {
        this.install = install;
    }

    public long getInstallTime() {
        return installTime;
    }

    public void setInstallTime(long installTime) {
        this.installTime = installTime;
    }

    public long getUninstallTime() {
        return uninstallTime;
    }

    public void setUninstallTime(long uninstallTime) {
        this.uninstallTime = uninstallTime;
    }

    public String getTickerText() {
        return tickerText;
    }

    public void setTickerText(String tickerText) {
        this.tickerText = tickerText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "ApplicationInfo{" +
                "_id=" + _id +
                ", fileName='" + fileName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", downloadPath='" + downloadPath + '\'' +
                ", silence=" + silence +
                ", download=" + download +
                ", install=" + install +
                ", installTime=" + installTime +
                ", uninstallTime=" + uninstallTime +
                ", tickerText='" + tickerText + '\'' +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
