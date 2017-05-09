/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.domain.IdentifierMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;

/**
 * @author Chris Cranford
 */
public abstract class AbstractIdentifierMapping implements IdentifierMapping {
	private List<PersistentAttributeMapping> identifierAttributeMappings = new ArrayList<>();

	@Override
	public List<PersistentAttributeMapping> getPersistentAttributeMappings() {
		return Collections.unmodifiableList( identifierAttributeMappings );
	}

	protected void addPersistentAttributeMapping(PersistentAttributeMapping persistentAttributeMapping) {
		identifierAttributeMappings.add( persistentAttributeMapping );
	}

	protected void addPersistentAttributeMappings(List<PersistentAttributeMapping> persistentAttributeMappings) {
		identifierAttributeMappings.addAll( persistentAttributeMappings );
	}
}
