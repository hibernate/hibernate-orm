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
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.SqmQuerySpecCreationProcessingState;
import org.hibernate.query.sqm.produce.internal.SqmQuerySpecCreationProcessingStateStandardImpl;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmCreationOptions;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
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

	public static SqmQuerySpec generateSqmEntityIdSelect(
			SqmDeleteOrUpdateStatement sqmStatement,
			SessionFactoryImplementor sessionFactory) {
		final SqmIdSelectGenerator generator = new SqmIdSelectGenerator( sqmStatement, sessionFactory );

		return generator.process();
	}

	private final SqmDeleteOrUpdateStatement sourceSqmStatement;
	private final EntityTypeDescriptor entityDescriptor;
	private final SqmCreationContext creationContext;

	public SqmIdSelectGenerator(
			SqmDeleteOrUpdateStatement sourceSqmStatement,
			SqmCreationContext creationContext) {
		this.sourceSqmStatement = sourceSqmStatement;
		this.entityDescriptor = sourceSqmStatement.getTarget().getReferencedNavigable();
		this.creationContext = creationContext;
	}

	private SqmQuerySpec process() {
		final SqmQuerySpec sqmQuerySpec = new SqmQuerySpec();

		final SqmFromClause sqmFromClause = new SqmFromClause();
		sqmQuerySpec.setFromClause( sqmFromClause );

		final SqmRoot<Object> sqmRoot = new SqmRoot<>( entityDescriptor, toString() );
		sqmFromClause.addRoot( sqmRoot );

		final SqmSelectClause sqmSelectClause = new SqmSelectClause( true );
		sqmQuerySpec.setSelectClause( sqmSelectClause );
		applySelections( sqmSelectClause, sqmRoot );

		if ( sourceSqmStatement.getWhereClause() != null ) {
			sqmQuerySpec.setWhereClause( new SqmWhereClause( sourceSqmStatement.getWhereClause().getPredicate() ) );
		}

		return sqmQuerySpec;
	}

	private void applySelections(SqmSelectClause sqmSelectClause, SqmRoot<Object> sqmRoot) {
		final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>();

		final SqmCreationState sqmCreationState = new SqmCreationState() {
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

			@Override
			public String generateUniqueIdentifier() {
				return null;
			}

			@Override
			public ImplicitAliasGenerator getImplicitAliasGenerator() {
				return null;
			}

			@Override
			public SqmQuerySpecCreationProcessingState getCurrentQuerySpecProcessingState() {
				return (SqmQuerySpecCreationProcessingState) processingStateStack.getCurrent();
			}
		};

		final SqmQuerySpecCreationProcessingStateStandardImpl processingState = new SqmQuerySpecCreationProcessingStateStandardImpl(
				null,
				sqmCreationState
		);
		processingStateStack.push( processingState );

		sqmSelectClause.addSelection(
				new SqmSelection( entityDescriptor.getIdentifierDescriptor().createSqmExpression( sqmRoot, sqmCreationState ) )
		);
	}

}
