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

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.reveng.ide.completion.HQLCodeAssist;
import org.hibernate.tool.orm.jbt.api.wrp.ConfigurationWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.HqlCodeAssistWrapper;
import org.hibernate.tool.orm.jbt.internal.util.HqlCompletionRequestor;
import org.hibernate.tool.orm.jbt.internal.util.MetadataHelper;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class HqlCodeAssistWrapperFactory {

	public static HqlCodeAssistWrapper createHqlCodeAssistWrapper(ConfigurationWrapper configurationWrapper) {
		return createHqlCodeAssistWrapper(
				MetadataHelper.getMetadata((Configuration)configurationWrapper.getWrappedObject()));
	}
	
	private static HqlCodeAssistWrapper createHqlCodeAssistWrapper(Metadata m) {
		return new HqlCodeAssistWrapperImpl(m);
	}
	
	private static class HqlCodeAssistWrapperImpl 
			extends AbstractWrapper
			implements HqlCodeAssistWrapper {
		
		private HQLCodeAssist hqlCodeAssist = null;
		
		private HqlCodeAssistWrapperImpl(Metadata metadata) {
			this.hqlCodeAssist = new HQLCodeAssist(metadata);
		}
		
		@Override public HQLCodeAssist getWrappedObject() {
			return hqlCodeAssist; 
		}
		
		@Override
		public void codeComplete(String query, int position, Object handler) {
			hqlCodeAssist.codeComplete(
					query, 
					position, 
					new HqlCompletionRequestor(handler));
		}

	}

}
