/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;

/**
 * @author Chris Cranford
 */
public class IdentifierMappingSingle extends AbstractIdentifierMapping {
	public IdentifierMappingSingle(PersistentAttributeMapping persistentAttributeMapping) {
		addPersistentAttributeMapping( persistentAttributeMapping );
	}

	@Override
	public boolean isSingleIdentifierMapping() {
		return true;
	}

	@Override
	public boolean isEmbeddedIdentifierMapping() {
		return false;
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
