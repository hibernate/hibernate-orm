/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan.spi.CompositeAttributeFetch;
import org.hibernate.loader.plan.spi.EntityIdentifierDescription;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AttributeDefinition;
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
		final ExpandingCompositeQuerySpace querySpace = expandingEntityQuerySpace().makeCompositeIdentifierQuerySpace();
		return identifierDefinition.isEncapsulated()
				? buildEncapsulatedCompositeIdentifierDescription( querySpace )
				: buildNonEncapsulatedCompositeIdentifierDescription( querySpace );
	}

	private NonEncapsulatedEntityIdentifierDescription buildNonEncapsulatedCompositeIdentifierDescription(
			ExpandingCompositeQuerySpace compositeQuerySpace) {
		return new NonEncapsulatedEntityIdentifierDescription(
				this,
				compositeQuerySpace,
				(CompositeType) getEntityPersister().getIdentifierType(),
				getPropertyPath().append( EntityPersister.ENTITY_ID )
		);
	}

	private EncapsulatedEntityIdentifierDescription buildEncapsulatedCompositeIdentifierDescription(
			ExpandingCompositeQuerySpace compositeQuerySpace) {
		return new EncapsulatedEntityIdentifierDescription(
				this,
				compositeQuerySpace,
				(CompositeType) getEntityPersister().getIdentifierType(),
				getPropertyPath().append( EntityPersister.ENTITY_ID )
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

	protected CompositeAttributeFetch createCompositeAttributeFetch(
			AttributeDefinition attributeDefinition,
			ExpandingCompositeQuerySpace compositeQuerySpace) {
		return new CompositeAttributeFetchImpl(
				this,
				attributeDefinition,
				compositeQuerySpace,
				true
		);
	}
}
