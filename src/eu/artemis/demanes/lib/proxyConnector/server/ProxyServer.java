/**
 * File ProxyServer.java
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import eu.artemis.demanes.lib.MessageDispatcher;
import eu.artemis.demanes.lib.SocketConnector;
import eu.artemis.demanes.lib.impl.communication.CommUtils;
import eu.artemis.demanes.lib.impl.communication.SocketReader;
import eu.artemis.demanes.lib.proxyConnector.ProxyConnectorException;
import eu.artemis.demanes.logging.LogConstants;
import eu.artemis.demanes.logging.LogEntry;

/**
 * ProxyServer
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 6 feb. 2015
 *
 */
public class ProxyServer {

	private final Logger logger = Logger.getLogger("dmns:log");

	private final SocketConnector deviceConnector;

	private OutputStream outputStream;

	private InputStream inputStream;

	private ServerRunner serverRunner;

	private Thread serverThread;

	public ProxyServer(int proxyPort, SocketConnector deviceConnector)
			throws ProxyConnectorException {
		logger.info(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_INFO, "Comm",
				"Starting ProxyServer on port " + proxyPort));

		this.deviceConnector = deviceConnector;

		try {
			ServerSocket serverSocket = new ServerSocket(proxyPort);
			serverSocket.setReuseAddress(true);

			this.serverRunner = new ServerRunner(serverSocket);
			this.serverThread = new Thread(this.serverRunner);
			this.serverThread.start();
		} catch (IOException e) {
			logger.fatal(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_FATAL,
					"Error setting up ProxyServer", e));
			throw new ProxyConnectorException("Error setting up ProxyServer", e);
		}
	}

	public void stop() {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Comm", "Stopping ProxyServer"));

		if (this.serverRunner != null)
			this.serverRunner.stop();

		if (this.serverThread != null)
			this.serverThread.interrupt();
	}

	/**
	 * Provides the service to the physical device. A message is received, and
	 * in this function, the result is written to the outputstream (managed by
	 * the serverThread)
	 * 
	 * @see eu.artemis.demanes.lib.MessageDispatcher#dispatchMessage(java.nio.ByteBuffer
	 *      )
	 */
	public void sendToClient(ByteBuffer msg) {
		byte[] value = new byte[msg.remaining()];
		msg.get(value);

		if (this.outputStream == null) {
			logger.trace(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_TRACE, "Comm",
					"Attempting to write to client before connected... dropping message"));
			return;
		}

		logger.trace(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_TRACE,
				"Writing message to server stream: [" + CommUtils.asHex(value) + "0A]"));
		
		try {
			int len1 = value.length / 256;
			int len2 = value.length % 256;
			
			this.outputStream.write((byte) len1);
			this.outputStream.write((byte) len2);
			this.outputStream.write(value);
			this.outputStream.write(0x0A);
			this.outputStream.flush();
		} catch (IOException e) {
			logger.warn(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_WARN, "Comm",
					"Error sending message to Client", e));
		}
	}

	/**
	 * ServerRunner
	 *
	 * @author leeuwencjv
	 * @version 0.1
	 * @since 2 feb. 2015
	 *
	 */
	private class ServerRunner implements Runnable {

		private final ServerSocket serverSocket;

		private SocketReader reader;

		private Thread readerThread;

		/**
		 * @param proxyPort
		 * @throws IOException
		 */
		public ServerRunner(ServerSocket socket) {
			this.serverSocket = socket;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				while (true) {
					// Wait for an incoming connection
					logger.info(new LogEntry(this.getClass().getName(),
							LogConstants.LOG_LEVEL_INFO, "Comm",
							"Listening for incoming connections"));
					Socket clientSocket = this.serverSocket.accept();
					logger.info(new LogEntry(this.getClass().getName(),
							LogConstants.LOG_LEVEL_INFO, "Comm",
							"Received connection from "
									+ clientSocket.getInetAddress()));

					// Upon accepting a new connection, terminate the old thread
					if (this.reader != null)
						stopReader();

					// Get an input and an output stream to the client
					inputStream = new BufferedInputStream(
							clientSocket.getInputStream());
					outputStream = new BufferedOutputStream(
							clientSocket.getOutputStream());

					// Create reader thread
					this.reader = new SocketReader(inputStream, outputStream,
							new DeviceDispatcher());
					this.readerThread = new Thread(reader,
							"ProxyServer reader thread");
					this.readerThread.start();
				}
			} catch (IOException e) {
				logger.debug(new LogEntry(this.getClass().getName(),
						LogConstants.LOG_LEVEL_DEBUG, "Comm",
						"Stopping proxy server thread", e));
				this.stop();
			}
		}

		/**
		 * When the server thread stops, cleanup the Server socket and the
		 * corresponding reader thread.
		 */
		public void stop() {
			stopReader();

			try {
				this.serverSocket.close();
			} catch (IOException e) {
				logger.warn(new LogEntry(this.getClass().getName(),
						LogConstants.LOG_LEVEL_WARN, "Comm",
						"Exception occured during closing server socket", e));
			}
		}

		/**
		 * Stop the reader thread whenever a new one is created, or when the
		 * server thread is stopped. Cleans up the reader thread as well as the
		 * client sockets.
		 */
		public void stopReader() {
			logger.debug(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_DEBUG, "Comm",
					"Stopping previous ReaderThread (if exists)"));

			if (this.reader != null)
				this.reader.stop();
			this.reader = null;

			if (this.readerThread != null)
				this.readerThread.interrupt();
			this.readerThread = null;

			try {
				if (inputStream != null) {
					inputStream.close();
					outputStream.close();
				}
			} catch (IOException e) {
				logger.warn(new LogEntry(this.getClass().getName(),
						LogConstants.LOG_LEVEL_WARN, "Comm",
						"Exception occured during closing socket streams", e));
			}

			inputStream = null;
			outputStream = null;
		}
	}

	/**
	 * The DeviceDispatcher is provided to the SocketReader. When a message is
	 * received through the proxy, the DeviceDispatcher passes it on to the
	 * physical device connector.
	 *
	 * @author leeuwencjv
	 * @version 0.1
	 * @since 6 feb. 2015
	 *
	 */
	private class DeviceDispatcher implements MessageDispatcher {

		/**
		 * This function returns null. In theory it should return the response
		 * of the device connector, but the SocketReader (created by the
		 * ServerRunner) already takes care of this.
		 * 
		 * @see eu.artemis.demanes.lib.MessageDispatcher#dispatchMessage(java.nio
		 *      .ByteBuffer)
		 */
		@Override
		public ByteBuffer dispatchMessage(ByteBuffer msg) {
			// Copy the message
			byte[] value = new byte[msg.remaining()];
			msg.get(value, 0, msg.remaining());

			// Put in the device connector
			deviceConnector.write(value);
			return null;
		}

	}

}
