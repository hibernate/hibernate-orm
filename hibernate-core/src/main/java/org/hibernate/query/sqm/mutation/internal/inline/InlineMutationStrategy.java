/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import java.util.function.Function;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandler;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

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
	public MultiTableHandlerBuildResult buildHandler(SqmDeleteOrUpdateStatement<?> sqmStatement, DomainParameterXref domainParameterXref, DomainQueryExecutionContext context) {
		final MutableObject<JdbcParameterBindings> firstJdbcParameterBindings = new MutableObject<>();
		final MultiTableHandler multiTableHandler = sqmStatement instanceof SqmDeleteStatement<?> sqmDelete
				? buildHandler( sqmDelete, domainParameterXref, context, firstJdbcParameterBindings )
				: buildHandler( (SqmUpdateStatement<?>) sqmStatement, domainParameterXref, context, firstJdbcParameterBindings );
		return new MultiTableHandlerBuildResult( multiTableHandler, firstJdbcParameterBindings.get() );
	}

	public MultiTableHandler buildHandler(SqmUpdateStatement<?> sqmUpdate, DomainParameterXref domainParameterXref, DomainQueryExecutionContext context, MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		return new InlineUpdateHandler(
				matchingIdsStrategy.apply( sqmUpdate ),
				sqmUpdate,
				domainParameterXref,
				context,
				firstJdbcParameterBindingsConsumer
		);
	}

	public MultiTableHandler buildHandler(SqmDeleteStatement<?> sqmDelete, DomainParameterXref domainParameterXref, DomainQueryExecutionContext context, MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		return new InlineDeleteHandler(
				matchingIdsStrategy.apply( sqmDelete ),
				sqmDelete,
				domainParameterXref,
				context,
				firstJdbcParameterBindingsConsumer
		);
	}
}
