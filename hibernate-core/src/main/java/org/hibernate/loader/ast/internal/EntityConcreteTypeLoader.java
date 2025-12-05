/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;


import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.WrongClassException;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static java.util.Collections.singletonList;

/**
 * Utility class that caches the SQL AST needed to read the discriminator value
 * associated with the provided {@link EntityPersister} and returns the
 * resolved concrete entity type.
 *
 * @author Marco Belladelli
 * @see ConcreteProxy
 */
public class EntityConcreteTypeLoader {
	private final EntityMappingType entityDescriptor;
	private final SelectStatement sqlSelect;
	private final JdbcParametersList jdbcParameters;

	public EntityConcreteTypeLoader(EntityMappingType entityDescriptor, SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		final var builder = JdbcParametersList.newBuilder();
		sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				singletonList( entityDescriptor.getDiscriminatorMapping() ),
				entityDescriptor.getIdentifierMapping(),
				null,
				1,
				new LoadQueryInfluencers( sessionFactory ),
				new LockOptions(),
				builder::add,
				sessionFactory
		);
		jdbcParameters = builder.build();
	}

	public EntityMappingType getConcreteType(Object id, SharedSessionContractImplementor session) {
		final var factory = session.getSessionFactory();

		final var bindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final int offset = bindings.registerParametersForEachJdbcValue(
				id,
				entityDescriptor.getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

		final var jdbcSelect =
				factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( factory, sqlSelect )
						.translate( bindings, QueryOptions.NONE );
		final var results =
				session.getFactory().getJdbcServices().getJdbcSelectExecutor()
						.list(
								jdbcSelect,
								bindings,
								new BaseExecutionContext( session ),
								RowTransformerStandardImpl.instance(),
								null,
								ListResultsConsumer.UniqueSemantic.NONE,
								1
						);

		if ( results.isEmpty() ) {
			throw new ObjectNotFoundException( entityDescriptor.getEntityName(), id );
		}
		else {
			assert results.size() == 1;
			final Object result = results.get( 0 );
			final var mappingMetamodel = factory.getMappingMetamodel();
			final var concreteType =
					result instanceof Class<?> concreteClass
							? mappingMetamodel.getEntityDescriptor( concreteClass )
							: mappingMetamodel.getEntityDescriptor( (String) result );
			if ( !concreteType.isTypeOrSuperType( entityDescriptor ) ) {
				throw new WrongClassException(
						concreteType.getEntityName(),
						id,
						entityDescriptor.getEntityName(),
						result
				);
			}
			return concreteType;
		}
	}
}
