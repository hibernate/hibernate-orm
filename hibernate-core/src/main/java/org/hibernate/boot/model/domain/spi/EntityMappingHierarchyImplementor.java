/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.spi;

import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.IdentifierMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;

/**
 * @author Chris Cranford
 */
public interface EntityMappingHierarchyImplementor extends EntityMappingHierarchy {
	@Override
	IdentifierMappingImplementor getIdentifierMapping();

	void setIdentifierAttributeMapping(PersistentAttributeMapping identifierAttributeMapping);
	void setVersionAttributeMapping(PersistentAttributeMapping versionAttributeMapping);

	void setIdentifierMapping(IdentifierMapping identifierMapping);

	void setDiscriminatorMapping(ValueMapping discriminatorMapping);
}
