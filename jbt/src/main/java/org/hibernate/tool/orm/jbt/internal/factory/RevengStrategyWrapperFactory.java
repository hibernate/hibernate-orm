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
package org.hibernate.tool.orm.jbt.internal.factory;

import java.lang.reflect.Constructor;

import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.orm.jbt.api.wrp.RevengSettingsWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.RevengStrategyWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.Wrapper;
import org.hibernate.tool.orm.jbt.internal.util.ReflectUtil;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class RevengStrategyWrapperFactory {

	public static RevengStrategyWrapper createRevengStrategyWrapper(Object...objects) {
		RevengStrategy wrappedRevengStrategy = null;
		if (objects.length == 0) {
			wrappedRevengStrategy = createDefaultStrategy();
		} else if (objects.length == 2) {
			wrappedRevengStrategy = createDelegatingStrategy((String)objects[0], (RevengStrategy)((Wrapper)objects[1]).getWrappedObject());
		} else {
			throw new RuntimeException("RevengStrategyWrapperFactory#create has either 0 or 2 arguments");
		}
		return createRevengStrategyWrapper(wrappedRevengStrategy);
	}
	
	private static RevengStrategy createDefaultStrategy() {
		return new DefaultStrategy();
	}
	
	private static RevengStrategy createDelegatingStrategy(String strategyClassName, RevengStrategy delegate) {
		Class<?> revengStrategyClass = ReflectUtil.lookupClass(strategyClassName);
		Constructor<?> constructor = null;
		for (Constructor<?> c : revengStrategyClass.getConstructors()) {
			if (c.getParameterCount() == 1 && 
					c.getParameterTypes()[0].isAssignableFrom(RevengStrategy.class)) {
				constructor = c;
				break;
			}
		}
		if (constructor != null) {
			return (RevengStrategy)ReflectUtil.createInstance(
					strategyClassName, 
					new Class[] { RevengStrategy.class }, 
					new Object[] { delegate });
		} else {
			return (RevengStrategy)ReflectUtil.createInstance(strategyClassName);
		}
	}

	static RevengStrategyWrapper createRevengStrategyWrapper(RevengStrategy wrappedRevengStrategy) {
		return new RevengStrategyWrapperImpl(wrappedRevengStrategy);
	}
	
	private static class RevengStrategyWrapperImpl 
			extends AbstractWrapper
			implements RevengStrategyWrapper {
		
		private RevengStrategy revengStrategy = null;
		
		private RevengStrategyWrapperImpl(RevengStrategy revengStrategy) {
			this.revengStrategy = revengStrategy;
		}
		
		@Override 
		public RevengStrategy getWrappedObject() { 
			return revengStrategy; 
		}
		
		@Override
		public void setSettings(RevengSettingsWrapper revengSettings) { 
			revengStrategy.setSettings((RevengSettings)revengSettings.getWrappedObject()); 
		}
		
	}

}
