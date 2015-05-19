/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.spaces;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan.spi.JoinDefinedByMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class EntityQuerySpaceImpl extends AbstractExpandingSourceQuerySpace implements ExpandingEntityQuerySpace {
	private final EntityPersister persister;

	public EntityQuerySpaceImpl(
			EntityPersister persister,
			String uid,
			ExpandingQuerySpaces querySpaces,
			boolean canJoinsBeRequired) {
		super( uid, Disposition.ENTITY, querySpaces, canJoinsBeRequired );
		this.persister = persister;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return super.sessionFactory();
	}

	@Override
	public PropertyMapping getPropertyMapping() {
		// entity persisters are typically PropertyMapping implementors, but this is part of the funky
		// "optional interface hierarchy" for entity persisters.  The internal ones all implement
		// PropertyMapping...
		return (PropertyMapping) persister;
	}

	@Override
	public String[] toAliasedColumns(String alias, String propertyName) {
		return getPropertyMapping().toColumns( alias, propertyName );
	}

	@Override
	public EntityPersister getEntityPersister() {
		return persister;
	}

	@Override
	public ExpandingCompositeQuerySpace makeCompositeIdentifierQuerySpace() {
		final String compositeQuerySpaceUid = getUid() + "-id";
		final ExpandingCompositeQuerySpace rhs = getExpandingQuerySpaces().makeCompositeQuerySpace(
				compositeQuerySpaceUid,
				new CompositePropertyMapping(
						(CompositeType) getEntityPersister().getIdentifierType(),
						(PropertyMapping) getEntityPersister(),
						getEntityPersister().getIdentifierPropertyName()
				),
				canJoinsBeRequired()
		);
		final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createCompositeJoin(
				this,
				EntityPersister.ENTITY_ID,
				rhs,
				canJoinsBeRequired(),
				(CompositeType) persister.getIdentifierType()
		);
		internalGetJoins().add( join );

		return rhs;
	}
}
