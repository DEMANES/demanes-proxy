/**
 * File ProxyDispatcher.java
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
package eu.artemis.demanes.lib.proxyConnector.server;

import java.nio.ByteBuffer;
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
import eu.artemis.demanes.lib.SocketConnector;
import eu.artemis.demanes.lib.proxyConnector.ProxyConnectorException;
import eu.artemis.demanes.logging.LogConstants;
import eu.artemis.demanes.logging.LogEntry;

/**
 * ProxyDispatcher
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 9 feb. 2015
 *
 */
@Component(immediate = true, properties = "type=proxy", designate = ProxyServerConfiguration.class, configurationPolicy = ConfigurationPolicy.require)
public class ProxyDispatcher implements MessageDispatcher {

	private final Logger logger = Logger.getLogger("dmns:log");

	private int port = 0;
	
	private ProxyServer server;

	private SocketConnector deviceConnector;

	@Activate
	public void activate(Map<?, ?> properties) throws ProxyConnectorException {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "LifeCycle", "Activating module"));

		ProxyServerConfiguration config = Configurable.createConfigurable(
				ProxyServerConfiguration.class, properties);

		// Check if we have a configuration
		if (config == null) {
			throw new ProxyConnectorException("No valid setup provided");
		} else if (config.getProxyPort() == 0) {
			throw new ProxyConnectorException("No proxy port provided");
		}

		this.port = config.getProxyPort();
		this.server = new ProxyServer(port, deviceConnector);
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
	
	private void disconnect() {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Comm", "Disconnecting server"));
		
		this.server.stop();
		this.server = null;
		this.port = 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.artemis.demanes.lib.MessageDispatcher#dispatchMessage(java.nio.ByteBuffer
	 * )
	 */
	@Override
	public ByteBuffer dispatchMessage(ByteBuffer msg) {
		this.server.sendToClient(msg);
		return null;
	}

	/**
	 * @param connector
	 */
	@Reference
	public void setConnector(SocketConnector connector) {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Reference",
				"Setting deviceConnector to " + connector));

		this.deviceConnector = connector;
	}

	public void unsetConnector(SocketConnector connector) {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Reference",
				"Removing deviceConnector " + connector));

		this.deviceConnector = null;
	}

	@Override
	public String toString() {
		return "ProxyServer port " + this.port;
	}

}
