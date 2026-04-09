/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
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
package org.hibernate.tool.orm.jbt.api.wrp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import org.hibernate.tool.reveng.ide.completion.HQLCodeAssist;
import org.hibernate.tool.orm.jbt.internal.factory.ConfigurationWrapperFactory;
import org.hibernate.tool.orm.jbt.internal.factory.HqlCodeAssistWrapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HqlCodeAssistWrapperTest {
	
	private HqlCodeAssistWrapper hqlCodeAssistWrapper = null;
	
	@BeforeEach
	public void beforeEach() {
		ConfigurationWrapper c = ConfigurationWrapperFactory.createNativeConfigurationWrapper();
		c.setProperty("hibernate.connection.url", "jdbc:h2:mem:test");
		hqlCodeAssistWrapper = HqlCodeAssistWrapperFactory.createHqlCodeAssistWrapper(c);
	}
	
	@Test
	public void testConstruction() {
		assertNotNull(hqlCodeAssistWrapper);
	}
	
	@Test
	public void testCodeComplete() throws Exception {
		// First test the handler's 'accept' method
		TestCodeCompletionHandler completionHandler = new TestCodeCompletionHandler();
		assertEquals(0, completionHandler.acceptCount);
		hqlCodeAssistWrapper.codeComplete("", 0, completionHandler);
		assertNotEquals(0, completionHandler.acceptCount);
		// Now try to invoke the handler's 'completionFailure' method
		Field f = hqlCodeAssistWrapper.getClass().getDeclaredField("hqlCodeAssist");
		f.setAccessible(true);
		f.set(hqlCodeAssistWrapper, new HQLCodeAssist(null));
		assertNull(completionHandler.errorMessage);
		hqlCodeAssistWrapper.codeComplete("FROM ", 5, completionHandler);
		assertNotNull(completionHandler.errorMessage);
	}
	
	static class TestCodeCompletionHandler {
		int acceptCount = 0;
		String errorMessage = null;
		public boolean accept(Object o) {
			acceptCount++;
			return false;
		}
		public void completionFailure(String errorMessage) {
			this.errorMessage = errorMessage;
		}
	}

}
