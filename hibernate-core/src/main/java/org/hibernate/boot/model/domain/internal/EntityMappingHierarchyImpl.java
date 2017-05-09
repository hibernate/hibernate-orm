/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.IdentifierMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.boot.model.domain.spi.EntityMappingHierarchyImplementor;

/**
 * @author Chris Cranford
 */
public class EntityMappingHierarchyImpl implements EntityMappingHierarchyImplementor {

	private IdentifierMapping identifierMapping;
	private PersistentAttributeMapping versionAttributeMapping;
	private ValueMapping discriminatorValueMapping;

	@Override
	public void setIdentifierMapping(IdentifierMapping identifierMapping) {
		this.identifierMapping = identifierMapping;
	}

	@Override
	public void setVersionAttributeMapping(PersistentAttributeMapping versionAttributeMapping) {
		this.versionAttributeMapping = versionAttributeMapping;
	}

	@Override
	public boolean hasIdentifierAttributeMapping() {
		return identifierMapping.isSingleIdentifierMapping() || identifierMapping.isEmbeddedIdentifierMapping();
	}

	@Override
	public boolean isVersioned() {
		return versionAttributeMapping != null;
	}

	@Override
	public ValueMapping getDiscriminatorMapping() {
		return discriminatorValueMapping;
	}
}
