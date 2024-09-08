/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.util.function.Function;

import org.hibernate.dialect.temptable.TemporaryTable;
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
	private final AfterUseAction afterUseAction;
	private final SqmDeleteStatement<?> sqmDelete;
	private final DomainParameterXref domainParameterXref;
	private final SessionFactoryImplementor sessionFactory;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;

	private final MultiTableSqmMutationConverter converter;

	public AbstractDeleteExecutionDelegate(
			EntityMappingType entityDescriptor,
			TemporaryTable idTable,
			AfterUseAction afterUseAction,
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			QueryParameterBindings queryParameterBindings,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.idTable = idTable;
		this.afterUseAction = afterUseAction;
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
				getSessionFactory()
		);
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	public TemporaryTable getIdTable() {
		return idTable;
	}

	public AfterUseAction getAfterUseAction() {
		return afterUseAction;
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
