/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import java.util.function.Function;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * Support for multi-table SQM mutation operations which select the matching id values from the database back into
 * the VM and uses that list of values to produce a restriction for the mutations.  The exact form of that
 * restriction is based on the {@link MatchingIdRestrictionProducer} implementation used
 *
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class InlineMutationStrategy implements SqmMultiTableMutationStrategy {
	private final Function<SqmDeleteOrUpdateStatement<?>,MatchingIdRestrictionProducer> matchingIdsStrategy;

	public InlineMutationStrategy(Dialect dialect) {
		this( determinePredicateProducer( dialect ) );
	}

	private static Function<SqmDeleteOrUpdateStatement<?>,MatchingIdRestrictionProducer> determinePredicateProducer(Dialect dialect) {
		return statement -> new InPredicateRestrictionProducer();
	}

	public InlineMutationStrategy(Function<SqmDeleteOrUpdateStatement<?>,MatchingIdRestrictionProducer> matchingIdsStrategy) {
		this.matchingIdsStrategy = matchingIdsStrategy;
	}

	@Override
	public int executeUpdate(
			SqmUpdateStatement<?> sqmUpdate,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		final InlineUpdateHandler handler = new InlineUpdateHandler(
				matchingIdsStrategy.apply( sqmUpdate ),
				sqmUpdate,
				domainParameterXref,
				context
		);
		return handler.execute( context );
	}

	@Override
	public int executeDelete(
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		final InlineDeleteHandler deleteHandler = new InlineDeleteHandler(
				matchingIdsStrategy.apply( sqmDelete ),
				sqmDelete,
				domainParameterXref,
				context
		);

		return deleteHandler.execute( context );
	}
}
