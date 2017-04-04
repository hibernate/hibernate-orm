/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * @author Gail Badner
 */
public class OneToManyLoadQueryDetails extends AbstractCollectionLoadQueryDetails {

	/**
	 * Constructs a EntityLoadQueryDetails object from the given inputs.
	 *
	 * @param loadPlan The load plan
	 * @param buildingParameters Any influencers that would affect the generated SQL (mostly we are concerned with those
	 * that add additional joins here)
	 * @param factory The SessionFactory
	 *
	 * @return The EntityLoadQueryDetails
	 */
	OneToManyLoadQueryDetails(
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
		return getElementEntityReferenceAliases().getTableAlias();
	}

	@Override
	protected void applyRootReturnSelectFragments(SelectStatementBuilder selectStatementBuilder) {

		selectStatementBuilder.appendSelectClauseFragment(
				getQueryableCollection().selectFragment(
						null,
						null,
						//getCollectionReferenceAliases().getCollectionTableAlias(),
						getElementEntityReferenceAliases().getTableAlias(),
						getElementEntityReferenceAliases().getColumnAliases().getSuffix(),
						getCollectionReferenceAliases().getCollectionColumnAliases().getSuffix(),
						true
				)
		);
		super.applyRootReturnSelectFragments( selectStatementBuilder );
	}

	@Override
	protected void applyRootReturnTableFragments(SelectStatementBuilder selectStatementBuilder) {
		final OuterJoinLoadable elementOuterJoinLoadable =
				(OuterJoinLoadable) getElementEntityReference().getEntityPersister();
		//final String tableAlias = getCollectionReferenceAliases().getCollectionTableAlias();
		final String tableAlias = getElementEntityReferenceAliases().getTableAlias();
		final String fragment =
				elementOuterJoinLoadable.fromTableFragment( tableAlias ) +
						elementOuterJoinLoadable.fromJoinFragment( tableAlias, true, true);
		selectStatementBuilder.appendFromClauseFragment( fragment );
	}

	private EntityReference getElementEntityReference() {
		return getRootCollectionReturn().getElementGraph().resolveEntityReference();
	}

	private EntityReferenceAliases getElementEntityReferenceAliases() {
		return getAliasResolutionContext().resolveEntityReferenceAliases( getElementEntityReference().getQuerySpaceUid() );
	}

}
