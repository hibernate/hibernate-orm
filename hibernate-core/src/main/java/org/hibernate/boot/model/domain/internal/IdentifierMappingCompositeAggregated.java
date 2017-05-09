/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;

/**
 * @author Chris Cranford
 */
public class IdentifierMappingCompositeAggregated extends AbstractIdentifierMapping {
	public IdentifierMappingCompositeAggregated(PersistentAttributeMapping persistentAttributeMapping) {
		addPersistentAttributeMapping( persistentAttributeMapping );
	}

	@Override
	public boolean isSingleIdentifierMapping() {
		return false;
	}

	@Override
	public boolean isEmbeddedIdentifierMapping() {
		return true;
	}

	@Override
	public PersistentAttributeMapping getSingularPersistentAttributeMapping() {
		assert getPersistentAttributeMappings().size() != 0;

		return getPersistentAttributeMappings().get( 0 );
	}

	@Override
	public ValueMapping getValueMapping() {
		return getSingularPersistentAttributeMapping().getValueMapping();
	}
}
