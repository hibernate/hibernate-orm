/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.WrongClassException;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
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
		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();
		sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				singletonList( discriminatorMapping ),
				entityDescriptor.getIdentifierMapping(),
				null,
				1,
				new LoadQueryInfluencers( sessionFactory ),
				LockOptions.NONE,
				jdbcParametersBuilder::add,
				sessionFactory
		);
		jdbcParameters = jdbcParametersBuilder.build();
	}

	public EntityMappingType getConcreteType(Object id, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getSessionFactory();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = sessionFactory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory();

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParamBindings.registerParametersForEachJdbcValue(
				id,
				entityDescriptor.getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

		final JdbcOperationQuerySelect jdbcSelect =
				sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, sqlSelect )
						.translate( jdbcParamBindings, QueryOptions.NONE );
		final List<Object> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParamBindings,
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
			final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel();
			final EntityPersister concreteType = result instanceof Class<?>
					? mappingMetamodel.getEntityDescriptor( (Class<?>) result )
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
