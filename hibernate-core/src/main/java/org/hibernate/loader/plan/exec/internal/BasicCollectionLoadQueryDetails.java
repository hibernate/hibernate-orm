/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.plan.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * @author Gail Badner
 */
public class BasicCollectionLoadQueryDetails extends AbstractCollectionLoadQueryDetails {

	/**
	 * Constructs a BasicCollectionLoadQueryDetails object from the given inputs.
	 *
	 * @param loadPlan The load plan
	 * @param buildingParameters And influencers that would affect the generated SQL (mostly we are concerned with those
	 * that add additional joins here)
	 * @param factory The SessionFactory
	 */
	BasicCollectionLoadQueryDetails(
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
							getCollectionReferenceAliases().getEntityElementAliases().getColumnAliases().getSuffix()
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
