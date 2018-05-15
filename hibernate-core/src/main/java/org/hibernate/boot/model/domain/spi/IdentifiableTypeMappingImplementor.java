/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain.spi;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface IdentifiableTypeMappingImplementor extends IdentifiableTypeMapping, ManagedTypeMappingImplementor {
	@Override
	EntityMappingHierarchyImplementor getEntityMappingHierarchy();

	@Override
	default RepresentationMode getExplicitRepresentationMode() {
		return getEntityMappingHierarchy().getExplicitRepresentationMode();
	}

	void injectSuperclassMapping(IdentifiableTypeMappingImplementor superTypeMapping);

	void setDeclaredIdentifierAttributeMapping(PersistentAttributeMapping declaredIdentifierAttributeMapping);

	void setDeclaredIdentifierEmbeddedValueMapping(EmbeddedValueMapping embeddedValueMapping);

	void setDeclaredVersionAttributeMapping(PersistentAttributeMapping declaredVersionAttributeMapping);

	default void setIdentifierAttributeMapping(PersistentAttributeMapping identifierAttributeMapping) {
		getEntityMappingHierarchy().setIdentifierAttributeMapping( identifierAttributeMapping );
	}

	default void setIdentifierEmbeddedValueMapping(EmbeddedValueMapping embeddedValueMapping) {
		getEntityMappingHierarchy().setIdentifierEmbeddedValueMapping( embeddedValueMapping );
	}

	default void setVersionAttributeMapping(PersistentAttributeMapping versionAttributeMapping) {
		getEntityMappingHierarchy().setVersionAttributeMapping( versionAttributeMapping );
	}

	int nextSubclassId();

	<X> IdentifiableTypeDescriptor<X> makeRuntimeDescriptor(
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext context);
}
