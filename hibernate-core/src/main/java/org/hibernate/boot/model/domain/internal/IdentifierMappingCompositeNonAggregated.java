/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;

/**
 * @author Chris Cranford
 */
public class IdentifierMappingCompositeNonAggregated extends AbstractIdentifierMapping {
	private EmbeddedValueMapping embeddedValueMapping;

	public IdentifierMappingCompositeNonAggregated(EmbeddedValueMapping embeddedValueMapping) {
		this.embeddedValueMapping = embeddedValueMapping;
	}

	@Override
	public boolean isSingleIdentifierMapping() {
		return false;
	}

	@Override
	public boolean isEmbeddedIdentifierMapping() {
		return false;
	}

	@Override
	public PersistentAttributeMapping getSingularPersistentAttributeMapping() {
		return null;
	}

	@Override
	public ValueMapping getValueMapping() {
		return embeddedValueMapping;
	}
}
