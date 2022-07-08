package com.redhat.rhjmc.prometheus_jfr_exporter;

import com.beust.jcommander.JCommander;
import org.eclipse.core.runtime.RegistryFactory;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.internal.KnownRecordingOptions;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;

public class Main {
	public static void main(String[] args) throws Exception {
		System.setProperty("org.openjdk.jmc.common.security.manager", SecurityManager.class.getName());
		RegistryFactory.setDefaultRegistryProvider(new RegistryProvider());
		Config config = null;
			CommandLineArgs cmd = new CommandLineArgs();
			JCommander jc = JCommander.newBuilder()
					.addObject(cmd)
					.build();
			jc.setProgramName("prometheus-jfr-exporter");
			try {
				jc.parse(args);
				if (cmd.isHelp()) {
					jc.usage();
					System.exit(0);
				}
				config = new Config();
				config.setDestination(cmd.getDestination());
				config.setDisk(Boolean.toString(cmd.isDisk()));
				config.setDumpOnExit(Boolean.toString(cmd.isDumpOnExit()));
				config.setEventConfiguration(cmd.getEventConfiguration());
				config.setMaxAge(cmd.getMaxAge());
				config.setMaxSize(cmd.getMaxSize());
				config.setName(cmd.getName());
				config.setJmxAddr(parseHost(cmd.getRemoteHost()));
				config.setHttpAddr(parseHost(cmd.getPrometheusHost()));
				config.setUserName(cmd.getUserName());
				config.setPassword(cmd.getPassword());
			} catch (Exception ex) {
				jc.usage();
				ex.printStackTrace(System.err);
				System.exit(1);
			}

		RecordingService rs = new RecordingService(config.getJmxAddr(), config.getRecordingOptions(),
				config.getEventConfiguration(), config.getUserName(), config.getPassword());
		rs.start();

		HttpService hs = new HttpService(config.getHttpAddr());
		hs.start();

		JfrCollector collector = new JfrCollector(rs);
		collector.register();
	}

	private static InetSocketAddress parseHost(String hostPort) {
		return parseHost(hostPort,null,null);
	}

	private static InetSocketAddress parseHost(String hostPort, String defaultHostname, Integer defaultPort) {
		String hostname = defaultHostname;
		Integer port = defaultPort;

		String[] hostnamePort = hostPort.split(":");
		if (hostnamePort.length > 2) {
			throw new IllegalArgumentException("invalid host: " + hostPort);
		}
		if (hostnamePort[0].length() > 0) {
			hostname = hostnamePort[0];
		}
		if (hostnamePort.length > 1 && hostnamePort[1].length() > 0) {
			try {
				port = Integer.parseInt(hostnamePort[1]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("invalid port: " + port);
			}
		}

		return new InetSocketAddress(hostname, port);
	}

	private static class Config {
		private String userName;
		private String password;
		private InetSocketAddress jmxAddr;
		private InetSocketAddress httpAddr = new InetSocketAddress("0.0.0.0", 8080);

		private IMutableConstrainedMap<String> recordingOptions = KnownRecordingOptions.OPTION_DEFAULTS_V2
				.emptyWithSameConstraints();
		private EventConfiguration eventConfiguration;

		public void setUserName(String userName) {
			this.userName = userName;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getUserName() {
			return userName;
		}

		public String getPassword() {
			return password;
		}

		public InetSocketAddress getJmxAddr() {
			return jmxAddr;
		}

		public InetSocketAddress getHttpAddr() {
			return httpAddr;
		}

		public IMutableConstrainedMap<String> getRecordingOptions() {
			return recordingOptions;
		}

		public EventConfiguration getEventConfiguration() {
			return eventConfiguration;
		}

		void setJmxAddr(InetSocketAddress jmxAddr) {
			this.jmxAddr = jmxAddr;
		}

		void setHttpAddr(InetSocketAddress httpAddr) {
			this.httpAddr = httpAddr;
		}

		void setDisk(String value) throws QuantityConversionException {
			this.recordingOptions.putPersistedString("disk", value);
		}

		void setDumpOnExit(String value) throws QuantityConversionException {
			this.recordingOptions.putPersistedString("dumpOnExit", value);
		}

		void setMaxAge(String value) throws QuantityConversionException {
			if (!value.equals("0")) this.recordingOptions.putPersistedString("maxAge", value);
		}

		void setMaxSize(String value) throws QuantityConversionException {
			if (!value.equals("0")) this.recordingOptions.putPersistedString("maxSize", value);
		}

		void setDestination(String value) throws QuantityConversionException {
			this.recordingOptions.putPersistedString("destination", value);
		}

		void setName(String value) throws QuantityConversionException {
			this.recordingOptions.putPersistedString("name", value);
		}

		void setEventConfiguration(String fileName) throws IOException, ParseException {
			if (fileName != null || !fileName.isEmpty()) {
				this.eventConfiguration = new EventConfiguration(EventConfiguration.createModel(new FileInputStream(fileName)));
			} else {
				this.eventConfiguration = new EventConfiguration(EventConfiguration.createModel(Main.class
						.getResourceAsStream("default.jfc")));
			}
		}
	}
}
