/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.internal.CacheableSqmInterpretation;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.Handler;
import org.hibernate.query.sqm.mutation.internal.MatchingIdSelectionHelper;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;


/**
 * DeleteHandler for the in-line strategy
 *
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
public abstract class AbstractInlineHandler implements Handler {
	private final MatchingIdRestrictionProducer matchingIdsPredicateProducer;
	private final EntityPersister entityDescriptor;
	private final DomainParameterXref domainParameterXref;
	private final CacheableSqmInterpretation<SelectStatement, JdbcOperationQuerySelect> matchingIdsInterpretation;

	protected AbstractInlineHandler(
			MatchingIdRestrictionProducer matchingIdsPredicateProducer,
			SqmDeleteOrUpdateStatement<?> sqmStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		final SessionFactoryImplementor sessionFactory = context.getSession().getFactory();
		final MappingMetamodel domainModel = sessionFactory.getMappingMetamodel();
		final String mutatingEntityName = sqmStatement.getTarget().getModel().getHibernateEntityName();
		this.entityDescriptor = domainModel.getEntityDescriptor( mutatingEntityName );

		this.domainParameterXref = domainParameterXref;
		this.matchingIdsPredicateProducer = matchingIdsPredicateProducer;
		this.matchingIdsInterpretation = MatchingIdSelectionHelper.createMatchingIdsSelect(
				sqmStatement,
				domainParameterXref,
				context,
				firstJdbcParameterBindingsConsumer
		);
	}

	@Override
	public JdbcParameterBindings createJdbcParameterBindings(DomainQueryExecutionContext context) {
		return SqmUtil.createJdbcParameterBindings(
				context.getQueryParameterBindings(),
				domainParameterXref,
				matchingIdsInterpretation.jdbcParamsXref(),
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) matchingIdsInterpretation.sqmParameterMappingModelTypes().get( parameter );
					}
				},
				context.getSession()
		);
	}

	@Override
	public boolean dependsOnParameterBindings() {
		return matchingIdsInterpretation.jdbcOperation().dependsOnParameterBindings();
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		return matchingIdsInterpretation.jdbcOperation().isCompatibleWith( jdbcParameterBindings, queryOptions );
	}

	public EntityPersister getEntityDescriptor() {
		return entityDescriptor;
	}

	protected MatchingIdRestrictionProducer getMatchingIdsPredicateProducer() {
		return matchingIdsPredicateProducer;
	}

	protected DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	protected CacheableSqmInterpretation<SelectStatement, JdbcOperationQuerySelect> getMatchingIdsInterpretation() {
		return matchingIdsInterpretation;
	}
}
