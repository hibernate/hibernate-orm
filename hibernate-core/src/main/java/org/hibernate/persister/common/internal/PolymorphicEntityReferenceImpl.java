/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.internal;

import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sqm.domain.SqmExpressableTypeEntity;
import org.hibernate.sqm.domain.SqmExpressableTypeEntityPolymorphicEntity;
import org.hibernate.sqm.domain.SqmNavigable;
import org.hibernate.sqm.domain.SqmNavigableSource;
import org.hibernate.sqm.domain.type.SqmDomainTypeEntity;

/**
 * @author Steve Ebersole
 */
public class PolymorphicEntityReferenceImpl implements SqmExpressableTypeEntityPolymorphicEntity {
	private final String requestedName;
	private final Set<EntityPersister<?>> implementors;


	public PolymorphicEntityReferenceImpl(String requestedName, Set<EntityPersister<?>> implementors) {
		this.requestedName = requestedName;
		this.implementors = implementors;
	}

	@Override
	public Set<SqmExpressableTypeEntity> getImplementors() {
		return implementors.stream().collect( Collectors.toSet() );
	}

	@Override
	public SqmDomainTypeEntity getExportedDomainType() {
		return null;
	}

	@Override
	public SqmNavigable findNavigable(String navigableName) {
		// only return navigables that all of the implementors define
		SqmNavigable sqmNavigable = null;
		for ( EntityPersister implementor : implementors ) {
			final SqmNavigable current = implementor.findNavigable( navigableName );
			if ( current == null ) {
				return null;
			}
			if ( sqmNavigable == null ) {
				sqmNavigable = current;
			}
		}

		return sqmNavigable;
	}

	@Override
	public String asLoggableText() {
		return "PolymorphicEntityReference( " + requestedName + ")";
	}

	@Override
	public String getEntityName() {
		return requestedName;
	}

	@Override
	public SqmNavigableSource getSource() {
		return null;
	}

	@Override
	public String getNavigableName() {
		return getEntityName();
	}
}
