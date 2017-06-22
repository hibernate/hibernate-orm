/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * @author Steve Ebersole
 */
public class EntityMapInstantiator extends AbstractMapInstantiator {
	private final Set<String> entityNamesToMatch = new HashSet<>();

	public EntityMapInstantiator(
			EntityMapping entityMapping,
			EntityDescriptor entityDescriptor) {
		super( entityDescriptor.getNavigableRole() );

		entityNamesToMatch.add( entityMapping.getEntityName() );
		visitSubtypes( entityMapping );
	}

	private void visitSubtypes(IdentifiableTypeMapping mapping) {
		for ( IdentifiableTypeMapping subtype : mapping.getSubTypeMappings() ) {
			if ( MappedSuperclassMapping.class.isInstance( subtype ) ) {
				final MappedSuperclassMapping mappedSuperclassMapping = (MappedSuperclassMapping) subtype;
				entityNamesToMatch.add( mappedSuperclassMapping.getName() );
			}
			else {
				final EntityMapping entityMapping = (EntityMapping) subtype;
				entityNamesToMatch.add( entityMapping.getEntityName() );
			}

			visitSubtypes( subtype );
		}
	}

	@Override
	protected boolean isInstanceByTypeValue(String extractedType) {
		return entityNamesToMatch.contains( extractedType );
	}

	@Override
	public Object instantiate(SharedSessionContractImplementor session) {
		return instantiateMap( session );
	}
}
