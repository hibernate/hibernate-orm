/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.SqmQuerySpecCreationProcessingStateStandardImpl;
import org.hibernate.query.sqm.mutation.internal.idtable.IdTableSessionUidColumn;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.StringType;

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

	public static SqmQuerySpec generateSqmEntityIdSelect(
			SqmDeleteOrUpdateStatement sqmStatement,
			ExecutionContext executionContext,
			SessionFactoryImplementor sessionFactory) {
		final SqmIdSelectGenerator generator = new SqmIdSelectGenerator( sqmStatement, executionContext, sessionFactory );
		return generator.process();
	}

	private final SqmDeleteOrUpdateStatement sourceSqmStatement;
	private final ExecutionContext executionContext;
	private final SqmCreationContext creationContext;
	private final EntityDomainType entityType;

	public SqmIdSelectGenerator(
			SqmDeleteOrUpdateStatement sourceSqmStatement,
			ExecutionContext executionContext,
			SqmCreationContext creationContext) {
		this.sourceSqmStatement = sourceSqmStatement;
		this.executionContext = executionContext;
		this.creationContext = creationContext;

		final String targetEntityName = sourceSqmStatement.getTarget().getEntityName();
		this.entityType = creationContext.getJpaMetamodel().entity( targetEntityName );
	}

	private SqmQuerySpec process() {
		final SqmQuerySpec sqmQuerySpec = new SqmQuerySpec( creationContext.getNodeBuilder() );


		final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>();

		final SqmCreationState creationState = new SqmCreationState() {
			@Override
			public SqmCreationContext getCreationContext() {
				return creationContext;
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
		final SqmSelectStatement sqmSelectStatement = new SqmSelectStatement( creationContext.getNodeBuilder() );
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
		final SqmRoot<?> sqmRoot = new SqmRoot( entityType, null, sourceSqmStatement.nodeBuilder() );
		sqmFromClause.addRoot( sqmRoot );

		final SqmSelectClause sqmSelectClause = new SqmSelectClause( true, creationContext.getNodeBuilder() );
		sqmQuerySpec.setSelectClause( sqmSelectClause );
		applySelections( sqmQuerySpec, sqmRoot, processingState );

		if ( sourceSqmStatement.getWhereClause() != null ) {
			sqmQuerySpec.applyPredicate( sourceSqmStatement.getWhereClause().getPredicate() );
		}

		return sqmQuerySpec;
	}

	private void applySelections(
			SqmQuerySpec sqmQuerySpec,
			SqmRoot<?> sqmRoot,
			SqmCreationProcessingState processingState) {
		//noinspection unchecked
		final SqmPath idPath = entityType.getIdentifierDescriptor().createSqmPath( sqmRoot, processingState.getCreationState() );

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
