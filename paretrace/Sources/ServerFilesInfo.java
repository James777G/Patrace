package com.arm.pa.paretrace.Sources;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ServerFilesInfo {

    private String hostname;
    private int port;

    private String username;

    private String password;

    private String cloudDir;

    private String invalidFile;
    private Map<String, List<String>> files;



    public ServerFilesInfo(){}

    public ServerFilesInfo(String hostname, int port, String username, String password, String cloudDir, String invalidFile, Map<String, List<String>> files) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.cloudDir = cloudDir;
        this.invalidFile = invalidFile;
        this.files = files;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCloudDir() {
        return cloudDir;
    }

    public void setCloudDir(String cloudDir) {
        this.cloudDir = cloudDir;
    }

    public String getInvalidFile() {
        return invalidFile;
    }

    public void setInvalidFile(String invalidFile) {
        this.invalidFile = invalidFile;
    }

    public Map<String, List<String>> getFiles() {
        return files;
    }

    public void setFiles(Map<String, List<String>> files) {
        this.files = files;
    }


    @Override
    public String toString() {
        return "ServerFilesInfo{" +
                "hostname='" + hostname + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", cloudDir='" + cloudDir + '\'' +
                ", invalidFile='" + invalidFile + '\'' +
                ", files=" + files +
                '}';
    }

    public String getFullHostName(){
        return this.hostname + ":" + this.port;
    }

}
