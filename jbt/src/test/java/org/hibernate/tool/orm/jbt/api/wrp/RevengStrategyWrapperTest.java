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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;

import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.internal.reveng.strategy.AbstractStrategy;
import org.hibernate.tool.orm.jbt.internal.factory.RevengSettingsWrapperFactory;
import org.hibernate.tool.orm.jbt.internal.factory.RevengStrategyWrapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RevengStrategyWrapperTest {

	private RevengStrategy wrappedRevengStrategy = null;
	private RevengStrategyWrapper revengStrategyWrapper = null;
	
	@BeforeEach
	public void beforeEach() {
		revengStrategyWrapper = RevengStrategyWrapperFactory.createRevengStrategyWrapper();
		wrappedRevengStrategy = (RevengStrategy)revengStrategyWrapper.getWrappedObject();
	}
	
	@Test
	public void testConstruction() {
		assertNotNull(wrappedRevengStrategy);
		assertNotNull(revengStrategyWrapper);
	}
	
	
	@Test
	public void testSetSettings() throws Exception {
		RevengSettingsWrapper revengSettingsWrapper = RevengSettingsWrapperFactory.createRevengSettingsWrapper(null);
		RevengSettings revengSettings = (RevengSettings)revengSettingsWrapper.getWrappedObject();
		Field field = AbstractStrategy.class.getDeclaredField("settings");
		field.setAccessible(true);
		assertNotSame(field.get(wrappedRevengStrategy), revengSettings);
		revengStrategyWrapper.setSettings(revengSettingsWrapper);
		assertSame(field.get(wrappedRevengStrategy), revengSettings);
	}
	
}
