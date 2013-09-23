/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan2.build.internal.returns;

import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.EntityIdentifierDescription;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.EncapsulatedEntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityReference extends AbstractExpandingFetchSource implements EntityReference {

	private final EntityIdentifierDescription identifierDescription;

	public AbstractEntityReference(
			ExpandingEntityQuerySpace entityQuerySpace,
			PropertyPath propertyPath) {
		super( entityQuerySpace, propertyPath );
		this.identifierDescription = buildIdentifierDescription();
	}

	private ExpandingEntityQuerySpace expandingEntityQuerySpace() {
		return (ExpandingEntityQuerySpace) expandingQuerySpace();
	}

	/**
	 * Builds just the first level of identifier description.  This will be either a simple id descriptor (String,
	 * Long, etc) or some form of composite id (either encapsulated or not).
	 *
	 * @return the descriptor for the identifier
	 */
	private EntityIdentifierDescription buildIdentifierDescription() {
		final EntityIdentifierDefinition identifierDefinition = getEntityPersister().getEntityKeyDefinition();

		if ( identifierDefinition.isEncapsulated() ) {
			final EncapsulatedEntityIdentifierDefinition encapsulatedIdentifierDefinition = (EncapsulatedEntityIdentifierDefinition) identifierDefinition;
			final Type idAttributeType = encapsulatedIdentifierDefinition.getAttributeDefinition().getType();
			if ( ! CompositeType.class.isInstance( idAttributeType ) ) {
				return new SimpleEntityIdentifierDescriptionImpl();
			}
		}

		// if we get here, we know we have a composite identifier...
		final Join join = expandingEntityQuerySpace().makeCompositeIdentifierJoin();
		return identifierDefinition.isEncapsulated()
				? buildEncapsulatedCompositeIdentifierDescription( join )
				: buildNonEncapsulatedCompositeIdentifierDescription( join );
	}

	private NonEncapsulatedEntityIdentifierDescription buildNonEncapsulatedCompositeIdentifierDescription(Join compositeJoin) {
		return new NonEncapsulatedEntityIdentifierDescription(
				this,
				(ExpandingCompositeQuerySpace) compositeJoin.getRightHandSide(),
				(CompositeType) getEntityPersister().getIdentifierType(),
				getPropertyPath().append( "id" )
		);
	}

	private EncapsulatedEntityIdentifierDescription buildEncapsulatedCompositeIdentifierDescription(Join compositeJoin) {
		return new EncapsulatedEntityIdentifierDescription(
				this,
				(ExpandingCompositeQuerySpace) compositeJoin.getRightHandSide(),
				(CompositeType) getEntityPersister().getIdentifierType(),
				getPropertyPath().append( "id" )
		);
	}

	@Override
	public EntityReference resolveEntityReference() {
		return this;
	}

	@Override
	public EntityPersister getEntityPersister() {
		return expandingEntityQuerySpace().getEntityPersister();
	}

	@Override
	public EntityIdentifierDescription getIdentifierDescription() {
		return identifierDescription;
	}

	protected CompositeFetch createCompositeFetch(
			CompositeType compositeType,
			ExpandingCompositeQuerySpace compositeQuerySpace) {
		return new CompositeFetchImpl(
				this,
				compositeType,
				compositeQuerySpace,
				true,
				getPropertyPath()
		);
	}
}
