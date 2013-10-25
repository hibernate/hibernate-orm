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
package org.hibernate.loader.plan2.exec.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.plan2.exec.internal.AliasResolutionContextImpl;
import org.hibernate.loader.plan2.exec.internal.Helper;
import org.hibernate.loader.plan2.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan2.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan2.spi.CollectionQuerySpace;
import org.hibernate.loader.plan2.spi.CollectionReturn;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.JoinDefinedByMetadata;
import org.hibernate.loader.plan2.spi.LoadPlan;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * @author Gail Badner
 */
public class BasicCollectionLoadQueryDetails extends CollectionLoadQueryDetails {

	/**
	 * Constructs a EntityLoadQueryDetails object from the given inputs.
	 *
	 * @param loadPlan The load plan
	 * @param buildingParameters And influencers that would affect the generated SQL (mostly we are concerned with those
	 * that add additional joins here)
	 * @param factory The SessionFactory
	 *
	 * @return The EntityLoadQueryDetails
	 */
	public static CollectionLoadQueryDetails makeForBatching(
			LoadPlan loadPlan,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {
		final CollectionReturn rootReturn = Helper.INSTANCE.extractRootReturn( loadPlan, CollectionReturn.class );
		final AliasResolutionContextImpl aliasResolutionContext = new AliasResolutionContextImpl( factory );
		return new BasicCollectionLoadQueryDetails(
						loadPlan,
						aliasResolutionContext,
						rootReturn,
						buildingParameters,
						factory
				);
	}

	protected BasicCollectionLoadQueryDetails(
			LoadPlan loadPlan,
			AliasResolutionContextImpl aliasResolutionContext,
			CollectionReturn rootReturn,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {
		super(
				loadPlan,
				aliasResolutionContext,
				rootReturn,
				buildingParameters,
				factory
		);
		generate();
	}

	@Override
	protected String getRootTableAlias() {
		return getCollectionReferenceAliases().getCollectionTableAlias();
	}

	@Override
	protected void applyRootReturnSelectFragments(SelectStatementBuilder selectStatementBuilder) {
		selectStatementBuilder.appendSelectClauseFragment(
			getQueryableCollection().selectFragment(
					getCollectionReferenceAliases().getCollectionTableAlias(),
					getCollectionReferenceAliases().getCollectionColumnAliases().getSuffix()
			)
		);
		if ( getQueryableCollection().isManyToMany() ) {
			final OuterJoinLoadable elementPersister = (OuterJoinLoadable) getQueryableCollection().getElementPersister();
			selectStatementBuilder.appendSelectClauseFragment(
					elementPersister.selectFragment(
							getCollectionReferenceAliases().getElementTableAlias(),
							getCollectionReferenceAliases().getEntityElementColumnAliases().getSuffix()
					)
			);
		}
		super.applyRootReturnSelectFragments( selectStatementBuilder );
	}

	@Override
	protected void applyRootReturnTableFragments(SelectStatementBuilder selectStatementBuilder) {
		selectStatementBuilder.appendFromClauseFragment(
				getQueryableCollection().getTableName(),
				getCollectionReferenceAliases().getCollectionTableAlias()
		);
	}

	@Override
	protected void applyRootReturnOrderByFragments(SelectStatementBuilder selectStatementBuilder) {
		final String manyToManyOrdering = getQueryableCollection().getManyToManyOrderByString(
				getCollectionReferenceAliases().getElementTableAlias()
		);
		if ( StringHelper.isNotEmpty( manyToManyOrdering ) ) {
			selectStatementBuilder.appendOrderByFragment( manyToManyOrdering );
		}
		super.applyRootReturnOrderByFragments( selectStatementBuilder );
	}

}
