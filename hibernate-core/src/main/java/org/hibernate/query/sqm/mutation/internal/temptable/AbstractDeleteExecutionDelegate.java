/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.util.function.Function;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDeleteExecutionDelegate implements TableBasedDeleteHandler.ExecutionDelegate {
	private final EntityMappingType entityDescriptor;
	private final TemporaryTable idTable;
	private final TemporaryTableStrategy temporaryTableStrategy;
	private final boolean forceDropAfterUse;
	private final SqmDeleteStatement<?> sqmDelete;
	private final DomainParameterXref domainParameterXref;
	private final SessionFactoryImplementor sessionFactory;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;

	private final MultiTableSqmMutationConverter converter;

	public AbstractDeleteExecutionDelegate(
			EntityMappingType entityDescriptor,
			TemporaryTable idTable,
			TemporaryTableStrategy temporaryTableStrategy,
			boolean forceDropAfterUse,
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			QueryParameterBindings queryParameterBindings,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.idTable = idTable;
		this.temporaryTableStrategy = temporaryTableStrategy;
		this.forceDropAfterUse = forceDropAfterUse;
		this.sqmDelete = sqmDelete;
		this.domainParameterXref = domainParameterXref;
		this.sessionFactory = sessionFactory;
		this.sessionUidAccess = sessionUidAccess;

		this.converter = new MultiTableSqmMutationConverter(
				entityDescriptor,
				getSqmDelete(),
				getSqmDelete().getTarget(),
				getDomainParameterXref(),
				queryOptions,
				loadQueryInfluencers,
				queryParameterBindings,
				sessionFactory.getSqlTranslationEngine()
		);
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	public TemporaryTable getIdTable() {
		return idTable;
	}

	public TemporaryTableStrategy getTemporaryTableStrategy() {
		return temporaryTableStrategy;
	}

	public AfterUseAction getAfterUseAction() {
		return forceDropAfterUse ? AfterUseAction.DROP : temporaryTableStrategy.getTemporaryTableAfterUseAction();
	}

	public SqmDeleteStatement<?> getSqmDelete() {
		return sqmDelete;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public Function<SharedSessionContractImplementor, String> getSessionUidAccess() {
		return sessionUidAccess;
	}

	public MultiTableSqmMutationConverter getConverter() {
		return converter;
	}
}
