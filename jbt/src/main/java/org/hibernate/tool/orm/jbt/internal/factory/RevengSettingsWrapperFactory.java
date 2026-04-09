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

import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.orm.jbt.api.wrp.RevengSettingsWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.RevengStrategyWrapper;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class RevengSettingsWrapperFactory {
	
	public static RevengSettingsWrapper createRevengSettingsWrapper(RevengStrategyWrapper revengStrategyWrapper) {
		RevengStrategy revengStrategy = 
				revengStrategyWrapper == null ? 
						null : (RevengStrategy)revengStrategyWrapper.getWrappedObject();
		RevengSettings wrappedRevengSettings = new RevengSettings(revengStrategy);
		return createRevengSettingsWrapper(wrappedRevengSettings);
	}

	static RevengSettingsWrapper createRevengSettingsWrapper(RevengSettings wrappedRevengSettings) {
		return new RevengSettingsWrapperImpl(wrappedRevengSettings);
	}
	
	private static class RevengSettingsWrapperImpl 
			extends AbstractWrapper
			implements RevengSettingsWrapper {
		
		private RevengSettings revengSettings = null;
		
		private RevengSettingsWrapperImpl(RevengSettings revengSettings) {
			this.revengSettings = revengSettings;
		}
		
		@Override 
		public RevengSettings getWrappedObject() { 
			return revengSettings; 
		}
		
		@Override 
		public RevengSettingsWrapper setDefaultPackageName(String s) { 
			revengSettings.setDefaultPackageName(s); 
			return this;
		}
		
		@Override 
		public RevengSettingsWrapper setDetectManyToMany(boolean b) { 
			revengSettings.setDetectManyToMany(b); 
			return this;
		}
		
		@Override 
		public RevengSettingsWrapper setDetectOneToOne(boolean b) { 
			revengSettings.setDetectOneToOne(b); 
			return this;
		}
		
		@Override 
		public RevengSettingsWrapper setDetectOptimisticLock(boolean b) { 
			revengSettings.setDetectOptimisticLock(b); 
			return this;
		}

	}

}
