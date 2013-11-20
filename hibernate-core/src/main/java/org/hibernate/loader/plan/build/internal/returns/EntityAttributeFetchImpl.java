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
