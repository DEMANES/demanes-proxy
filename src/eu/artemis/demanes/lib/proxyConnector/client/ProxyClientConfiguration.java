/**
 * File ProxyServerConfiguration.java
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

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * ProxyServerConfiguration
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 6 okt. 2014
 *
 */
@OCD(name = "Configuration for the Proxy Connection")
public interface ProxyClientConfiguration {

	@AD(name = "Proxy Host", description = "The host IP or domain name", deflt = "", required = true)
	String getProxyHost();
	
	@AD(name = "Proxy Port", description = "The host port", deflt = "3435", required = true)
	int getProxyPort();
}
