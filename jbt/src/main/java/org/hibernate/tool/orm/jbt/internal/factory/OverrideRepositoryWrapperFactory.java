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

import java.io.File;

import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.reveng.internal.reveng.strategy.TableFilter;
import org.hibernate.tool.orm.jbt.api.wrp.OverrideRepositoryWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.RevengStrategyWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.TableFilterWrapper;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class OverrideRepositoryWrapperFactory {

	public static OverrideRepositoryWrapper createOverrideRepositoryWrapper() {
		return createOverrideRepositoryWrapper(new OverrideRepository());
	}
	
	static OverrideRepositoryWrapper createOverrideRepositoryWrapper(OverrideRepository wrappedOverrideRepository) {
		return new OverrideRepositoryWrapperImpl(wrappedOverrideRepository);
	}
	
	private static class OverrideRepositoryWrapperImpl 
			extends AbstractWrapper
			implements OverrideRepositoryWrapper {
		
		private OverrideRepository overrideRepository = null;
		
		private OverrideRepositoryWrapperImpl(OverrideRepository overrideRepository) {
			this.overrideRepository = overrideRepository;
		}
		
		@Override 
		public OverrideRepository getWrappedObject() { 
			return overrideRepository; 
		}
		
		@Override
		public void addFile(File file) {
			overrideRepository.addFile(file);
		}
		
		@Override
		public RevengStrategyWrapper getReverseEngineeringStrategy(RevengStrategyWrapper res) {
			return RevengStrategyWrapperFactory.createRevengStrategyWrapper(
					overrideRepository.getReverseEngineeringStrategy((RevengStrategy)res.getWrappedObject()));
		}
		
		@Override
		public void addTableFilter(TableFilterWrapper tf) {
			overrideRepository.addTableFilter((TableFilter)tf.getWrappedObject());
		}

	}

}
