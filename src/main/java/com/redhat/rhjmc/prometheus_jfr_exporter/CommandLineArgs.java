package com.redhat.rhjmc.prometheus_jfr_exporter;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;



public class CommandLineArgs {
    @Parameter(names = {"-r","-remote"}, description = "The remote host in [host:port] format", required = true)
    private String remoteHost = null;

    @Parameter(names = {"-l","-local"}, description = "The local host for prometheus scraping in [host:port] format", required = true)
    private String prometheusHost = null;

    @Parameter(names = "--maxAge", description = "Max age of a recording. Default \"0\"")
    private String maxAge = "0";

    @Parameter(names = "--maxSize", description = "Max size of a recording. Default \"0\"")
    private String maxSize = "0";

    @Parameter(names = "--destination", description = "The recording destination file. Default recording.jfr")
    private String destination = "recording.jfr";

    @Parameter(names = "--name", description = "The recording name. Default MyRecording")
    private String name = "MyRecording";

    @Parameter(names = "--eventConfiguration", description = "The event Configuration file name.")
    private String eventConfiguration = null;

    @Parameter(names = "--disk", description = "Write on disk boolean [true/false]")
    private boolean disk = true;

    @Parameter(names = "--dumpOnExit", description = "Dump to file when exit boolean [true/false]")
    private boolean dumpOnExit = false;

    @Parameter(names = "--help", help = true)
    private boolean help;

    @Parameter(names = "--username", description = "user name for connecting to remote host.")
    private String userName = null;

    @Parameter(names = "--password", description = "password for remote connection.")
    private String password = null;

    public String getRemoteHost() {
        return remoteHost;
    }

    public String getPrometheusHost() {
        return prometheusHost;
    }

    public String getMaxAge() {
        return maxAge;
    }

    public String getMaxSize() {
        return maxSize;
    }

    public String getDestination() {
        return destination;
    }

    public String getName() {
        return name;
    }

    public String getEventConfiguration() {
        return eventConfiguration;
    }

    public boolean isDisk() {
        return disk;
    }

    public boolean isDumpOnExit() {
        return dumpOnExit;
    }

    public boolean isHelp() {
        return help;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

}
