/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.plan.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public class EntityAttributeFetchImpl extends AbstractEntityReference implements EntityFetch {
	private final FetchSource fetchSource;
	private final AttributeDefinition fetchedAttribute;
	private final FetchStrategy fetchStrategy;

	public EntityAttributeFetchImpl(
			ExpandingFetchSource fetchSource,
			AssociationAttributeDefinition fetchedAttribute,
			FetchStrategy fetchStrategy,
			ExpandingEntityQuerySpace entityQuerySpace) {
		super(
				entityQuerySpace,
				fetchSource.getPropertyPath().append( fetchedAttribute.getName() )
		);

		this.fetchSource = fetchSource;
		this.fetchedAttribute = fetchedAttribute;
		this.fetchStrategy = fetchStrategy;
	}

	@Override
	public FetchSource getSource() {
		return fetchSource;
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public EntityType getFetchedType() {
		return (EntityType) fetchedAttribute.getType();
	}

	@Override
	public boolean isNullable() {
		return fetchedAttribute.isNullable();
	}

	@Override
	public String getAdditionalJoinConditions() {
		return null;
	}

	@Override
	public String[] toSqlSelectFragments(String alias) {
		return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
		// todo : allow bi-directional key-many-to-one fetches?
		//		those do cause problems in Loader; question is whether those are indicative of that situation or
		// 		of Loaders ability to handle it.
	}

	@Override
	public AttributeDefinition getFetchedAttributeDefinition() {
		return fetchedAttribute;
	}
}
