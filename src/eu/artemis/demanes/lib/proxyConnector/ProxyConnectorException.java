/**
 * File ProxyConnectorException.java
 * 
 * This file is part of the eu.artemis.demanes.lib.proxyConnector.client project.
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
package eu.artemis.demanes.lib.proxyConnector;

import java.io.IOException;

/**
 * ProxyConnectorException
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 2 feb. 2015
 *
 */
public class ProxyConnectorException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8857391458805778788L;

	/**
	 * 
	 */
	public ProxyConnectorException(String msg) {
		super(msg);
	}

	/**
	 * @param string
	 * @param e
	 */
	public ProxyConnectorException(String msg, IOException e) {
		super(msg, e);
	}
	
}
