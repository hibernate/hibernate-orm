/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal;

import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.internal.SqmQuerySpecCreationProcessingStateStandardImpl;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;

import org.jboss.logging.Logger;

/**
 * Helper used to generate the SELECT for selection of an entity's
 * identifier, here specifically intended to be used as the SELECT
 * portion of a multi-table SQM mutation
 *
 * @author Steve Ebersole
 */
public class SqmIdSelectGenerator {
	private static final Logger log = Logger.getLogger( SqmIdSelectGenerator.class );

	/**
	 * @asciidoc
	 *
	 * Generates a query-spec for selecting all ids matching the restriction defined as part
	 * of the user's update/delete query.  This query-spec is generally used:
	 *
	 * 		* to select all the matching ids via JDBC - see {@link SqmMutationStrategyHelper#selectMatchingIds}
	 * 		* as a sub-query restriction to insert rows into an "id table"
	 */
	public static SqmQuerySpec generateSqmEntityIdSelect(
			SqmDeleteOrUpdateStatement sqmStatement,
			SqmCreationContext sqmCreationContext) {
		final EntityDomainType entityDomainType = sqmStatement.getTarget().getModel();

		log.tracef( "Starting generation of entity-id SQM selection - %s", entityDomainType.getHibernateEntityName() );

		final SqmQuerySpec sqmQuerySpec = new SqmQuerySpec( sqmCreationContext.getNodeBuilder() );

		final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>();

		final SqmCreationState creationState = new SqmCreationState() {
			@Override
			public SqmCreationContext getCreationContext() {
				return sqmCreationContext;
			}

			@Override
			public SqmCreationOptions getCreationOptions() {
				return () -> false;
			}

			@Override
			public Stack<SqmCreationProcessingState> getProcessingStateStack() {
				return processingStateStack;
			}
		};


		// temporary - used just for creating processingState
		final SqmSelectStatement sqmSelectStatement = new SqmSelectStatement( sqmCreationContext.getNodeBuilder() );
		//noinspection unchecked
		sqmSelectStatement.setQuerySpec( sqmQuerySpec );

		final SqmCreationProcessingState processingState = new SqmQuerySpecCreationProcessingStateStandardImpl(
				null,
				sqmSelectStatement,
				creationState
		);

		processingStateStack.push( processingState );

		final SqmFromClause sqmFromClause = new SqmFromClause();
		sqmQuerySpec.setFromClause( sqmFromClause );


		//noinspection unchecked
//		final SqmRoot<?> sqmRoot = new SqmRoot( entityDomainType, null, sqmCreationContext.getNodeBuilder() );
		final SqmRoot<?> sqmRoot = sqmStatement.getTarget();

		log.debugf( "Using SqmRoot [%s] as root for entity id-select", sqmRoot );
		sqmFromClause.addRoot( sqmRoot );

		final SqmSelectClause sqmSelectClause = new SqmSelectClause( true, sqmCreationContext.getNodeBuilder() );
		sqmQuerySpec.setSelectClause( sqmSelectClause );
		applySelections( sqmQuerySpec, sqmRoot, processingState );

		if ( sqmStatement.getWhereClause() != null ) {
			sqmQuerySpec.applyPredicate( sqmStatement.getWhereClause().getPredicate() );
		}

		return sqmQuerySpec;
	}

	private static void applySelections(
			SqmQuerySpec sqmQuerySpec,
			SqmRoot<?> sqmRoot,
			SqmCreationProcessingState processingState) {
		//noinspection unchecked
		final SqmPath idPath = sqmRoot.getModel().getIdentifierDescriptor().createSqmPath( sqmRoot, processingState.getCreationState() );

		//noinspection unchecked
		sqmQuerySpec.getSelectClause().add(
				new SqmSelection(
						idPath,
						null,
						processingState.getCreationState().getCreationContext().getNodeBuilder()
				)
		);
	}

}
