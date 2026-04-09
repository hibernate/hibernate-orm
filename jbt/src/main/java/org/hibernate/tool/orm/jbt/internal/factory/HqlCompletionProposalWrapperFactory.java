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

import org.hibernate.mapping.Property;
import org.hibernate.tool.reveng.ide.completion.HQLCompletionProposal;
import org.hibernate.tool.orm.jbt.api.wrp.HqlCompletionProposalWrapper;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class HqlCompletionProposalWrapperFactory {

	public static HqlCompletionProposalWrapper createHqlCompletionProposalWrapper(
			final Object wrappedCompletionProposal) {
		return new HqlCompletionProposalWrapperImpl((HQLCompletionProposal)wrappedCompletionProposal);
	}
	
	private static class HqlCompletionProposalWrapperImpl 
			extends AbstractWrapper
			implements HqlCompletionProposalWrapper {
		
		private HQLCompletionProposal hqlCompletionProposal = null;
		
		private HqlCompletionProposalWrapperImpl(HQLCompletionProposal hqlCompletionProposal) {
			this.hqlCompletionProposal = hqlCompletionProposal;
		}
		
		@Override 
		public HQLCompletionProposal getWrappedObject() { return hqlCompletionProposal; }
		
		@Override 
		public String getCompletion() { return hqlCompletionProposal.getCompletion(); }

		@Override 
		public int getReplaceStart() { return hqlCompletionProposal.getReplaceStart(); }

		@Override 
		public int getReplaceEnd() { return hqlCompletionProposal.getReplaceEnd(); }

		@Override 
		public String getSimpleName() { return hqlCompletionProposal.getSimpleName(); }

		@Override 
		public int getCompletionKind() { return hqlCompletionProposal.getCompletionKind(); }

		@Override 
		public String getEntityName() { return hqlCompletionProposal.getEntityName(); }

		@Override 
		public String getShortEntityName() { return hqlCompletionProposal.getShortEntityName(); }

		@Override 
		public Property getProperty() { return hqlCompletionProposal.getProperty(); }

		@Override 
		public int aliasRefKind() { return HQLCompletionProposal.ALIAS_REF; }

		@Override 
		public int entityNameKind() { return HQLCompletionProposal.ENTITY_NAME; }

		@Override 
		public int propertyKind() { return HQLCompletionProposal.PROPERTY; }

		@Override 
		public int keywordKind() { return HQLCompletionProposal.KEYWORD; }

		@Override 
		public int functionKind() { return HQLCompletionProposal.FUNCTION; }

	}

}
