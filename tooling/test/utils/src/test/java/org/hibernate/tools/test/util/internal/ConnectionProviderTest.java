/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tools.test.util.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.junit.jupiter.api.Test;

public class ConnectionProviderTest {
	
	private ConnectionProvider connectionProvider = new org.hibernate.tools.test.util.internal.ConnectionProvider();
	
	@Test
	public void testGetConnection() {
		try {
			assertNotNull(connectionProvider.getConnection());
		} catch (Exception e) {
			fail();
		}
	}
	
	@Test
	public void testCloseConnection() {
		try {
			connectionProvider.closeConnection(null);
		} catch (Exception e) {
			fail();
		}
	}

}
