/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.spi;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.metamodel.model.domain.RepresentationMode;

/**
 * @author Chris Cranford
 */
public interface EntityMappingHierarchyImplementor extends EntityMappingHierarchy {
	void setRootType(IdentifiableTypeMapping rootIdentifiableType);

	void setIdentifierAttributeMapping(PersistentAttributeMapping identifierAttributeMapping);

	void setIdentifierEmbeddedValueMapping(EmbeddedValueMapping identifierEmbeddedValueMapping);

	void setVersionAttributeMapping(PersistentAttributeMapping versionAttributeMapping);

	void setDiscriminatorMapping(ValueMapping discriminatorMapping);

	void setEmbeddedIdentifier(boolean embeddedIdentifier);

	void setOptimisticLockStyle(OptimisticLockStyle optimisticLockStyle);

	void setExplicitRepresentationMode(RepresentationMode explicitRepresentationMode);
}
