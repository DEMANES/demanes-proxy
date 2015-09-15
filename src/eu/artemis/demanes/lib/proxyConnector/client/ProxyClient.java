/**
 * File ProxyClient.java
 * 
 * This file is part of the eu.artemis.demanes.lib.proxyConnector project.
 *
 * Copyright 2015 TNO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.artemis.demanes.lib.proxyConnector.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

import org.apache.log4j.Logger;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import eu.artemis.demanes.lib.MessageDispatcher;
import eu.artemis.demanes.lib.MessageDispatcherRegistry;
import eu.artemis.demanes.lib.SocketConnector;
import eu.artemis.demanes.lib.impl.communication.CommUtils;
import eu.artemis.demanes.lib.impl.communication.MultiDispatcherServer;
import eu.artemis.demanes.lib.impl.communication.SocketReader;
import eu.artemis.demanes.lib.proxyConnector.ProxyConnectorException;
import eu.artemis.demanes.logging.LogConstants;
import eu.artemis.demanes.logging.LogEntry;

/**
 * ProxyClient
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 6 feb. 2015
 *
 */
@Component(immediate = true, properties = "type=proxy", designate = ProxyClientConfiguration.class, configurationPolicy = ConfigurationPolicy.require)
public class ProxyClient implements SocketConnector {

	private final Logger logger = Logger.getLogger("dmns:log");

	private String proxyHost;

	private int proxyPort;

	private OutputStream outputStream;

	private InputStream inputStream;

	private final MultiDispatcherServer dispatcherServer;

	private SocketReader reader;

	private Thread readerThread;

	public ProxyClient() {
		this.dispatcherServer = new MultiDispatcherServer();
	}

	@Activate
	public void activate(Map<?, ?> properties) throws ProxyConnectorException {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "LifeCycle", "Activating module"));

		this.getConfig(properties);

		try {
			Socket socket = new Socket(this.proxyHost, this.proxyPort);
			this.outputStream = new BufferedOutputStream(
					socket.getOutputStream());
			this.inputStream = new BufferedInputStream(socket.getInputStream());
		} catch (UnknownHostException e) {
			logger.fatal(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_FATAL, "Unknown host "
							+ this.proxyHost + ":" + this.proxyPort, e));

			throw new ProxyConnectorException("Unknown host " + this.proxyHost
					+ ":" + this.proxyPort);
		} catch (IOException e) {
			logger.fatal(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_FATAL,
					"Error setting up proxyClient " + this.proxyHost + ":"
							+ this.proxyPort, e));

			throw new ProxyConnectorException("Error setting up proxyClient "
					+ this.proxyHost + ":" + this.proxyPort, e);
		}

		this.reader = new SocketReader(inputStream, outputStream,
				dispatcherServer);
		this.readerThread = new Thread(reader, "ProxyClient reader thread");
		this.readerThread.start();
	}

	@Modified
	public void modify(Map<?, ?> properties) throws ProxyConnectorException {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "LifeCycle", "Modifying module"));
		this.disconnect();
		this.activate(properties);
	}

	@Deactivate
	public void stop() {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "LifeCycle", "Stopping module"));
		this.disconnect();
	}

	/**
	 * Disconnects the client from the server
	 */
	private void disconnect() {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Comm", "Disconnecting client"));

		if (this.reader != null)
			this.reader.stop();

		if (this.readerThread != null)
			this.readerThread.interrupt();

		this.reader = null;
		this.readerThread = null;

		try {
			if (this.inputStream != null) {
				this.inputStream.close();
				this.outputStream.close();
			}
		} catch (IOException e) {
			logger.warn(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_WARN, "Comm",
					"Exception occured during closing socket streams", e));
		}

		this.inputStream = null;
		this.outputStream = null;
	}

	private void getConfig(Map<?, ?> properties) throws ProxyConnectorException {
		ProxyClientConfiguration config = Configurable.createConfigurable(
				ProxyClientConfiguration.class, properties);

		// Check if we have a configuration
		if (config == null) {
			logger.fatal(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_FATAL, "Config",
					"No valid setup provided"));
			throw new ProxyConnectorException("No valid setup provided");
		} else if (config.getProxyHost() == null) {
			logger.fatal(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_FATAL, "Config",
					"No proxy host provided"));
			throw new ProxyConnectorException("No proxy host provided");
		} else if (config.getProxyPort() == 0) {
			logger.fatal(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_FATAL, "Config",
					"No proxy port provided"));
			throw new ProxyConnectorException("No proxy port provided");
		}

		this.proxyHost = config.getProxyHost();
		this.proxyPort = config.getProxyPort();

		logger.info(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_INFO, "Config",
				"ProxyClient configured for host " + this.proxyHost + ":"
						+ this.proxyPort));
	}

	/**
	 * The message dispatcher is only required at the client side, as its task
	 * will be to feed back the incoming messages to the rest of the system.
	 * 
	 * @param dispatcher
	 */
	@Reference(type = '*', unbind = "removeDispatcher")
	public void addDispatcher(MessageDispatcher dispatcher) {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Reference",
				"Adding MessageDispatcher " + dispatcher));

		this.dispatcherServer.addDispatcher(dispatcher);
	}

	public void removeDispatcher(MessageDispatcher dispatcher) {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Reference",
				"Removing MessageDispatcher " + dispatcher));

		this.dispatcherServer.removeDispatcher(dispatcher);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.artemis.demanes.lib.SocketConnector#write(byte[])
	 */
	@Override
	public MessageDispatcherRegistry write(byte[] val) {
		try {
			logger.trace(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_TRACE,
					"Writing message to server stream: ["
							+ CommUtils.asHex(val) + "0A]"));
			int len1 = val.length / 256;
			int len2 = val.length % 256;

			this.outputStream.write((byte) len1);
			this.outputStream.write((byte) len2);
			this.outputStream.write(val);
			this.outputStream.write((byte) CommUtils.END_OF_MESSAGE);
			this.outputStream.flush();
		} catch (IOException e) {
			logger.error(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_ERROR, "Comm",
					"Exception occured during write", e));
		}

		return this.dispatcherServer;
	}

}
