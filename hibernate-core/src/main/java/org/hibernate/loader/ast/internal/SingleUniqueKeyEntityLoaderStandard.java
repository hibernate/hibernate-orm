/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static java.util.Collections.singletonList;

/**
 * @author Steve Ebersole
 */
public class SingleUniqueKeyEntityLoaderStandard<T> implements SingleUniqueKeyEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	private final ModelPart uniqueKeyAttribute;
	private final String uniqueKeyAttributePath;
	private final JdbcParametersList jdbcParameters;
	private final JdbcOperationQuerySelect jdbcSelect;

	public SingleUniqueKeyEntityLoaderStandard(
			EntityMappingType entityDescriptor,
			SingularAttributeMapping uniqueKeyAttribute,
			LoadQueryInfluencers loadQueryInfluencers) {
		this.entityDescriptor = entityDescriptor;
		this.uniqueKeyAttributePath = getAttributePath( uniqueKeyAttribute );
		if ( uniqueKeyAttribute instanceof ToOneAttributeMapping ) {
			this.uniqueKeyAttribute = ( (ToOneAttributeMapping) uniqueKeyAttribute ).getForeignKeyDescriptor();
		}
		else {
			this.uniqueKeyAttribute = uniqueKeyAttribute;
		}

		final SessionFactoryImplementor sessionFactory = entityDescriptor.getEntityPersister().getFactory();
		final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder();
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelectByUniqueKey(
				entityDescriptor,
				Collections.emptyList(),
				uniqueKeyAttribute,
				null,
				loadQueryInfluencers,
				LockOptions.NONE,
				builder::add,
				sessionFactory
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		this.jdbcParameters = builder.build();
		this.jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
	}

	private static String getAttributePath(AttributeMapping attribute) {
		ManagedMappingType declaringType = attribute.getDeclaringType();
		if ( declaringType instanceof EmbeddableMappingType ) {
			final StringBuilder sb = new StringBuilder();
			sb.append( attribute.getAttributeName() );
			do {
				final EmbeddableValuedModelPart embeddedValueMapping = ( (EmbeddableMappingType) declaringType ).getEmbeddedValueMapping();
				attribute = embeddedValueMapping.asAttributeMapping();
				if ( attribute == null ) {
					break;
				}
				sb.insert( 0, '.' );
				sb.insert( 0, attribute.getAttributeName() );
				declaringType = attribute.getDeclaringType();
			} while ( declaringType instanceof EmbeddableMappingType );
			return sb.toString();
		}
		return attribute.getAttributeName();
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Override
	public T load(
			Object ukValue,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				ukValue,
				uniqueKeyAttribute,
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();
		final List<Object> list = sessionFactory.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new SingleUKEntityLoaderExecutionContext( uniqueKeyAttributePath, ukValue, session, readOnly ),
				RowTransformerSingularReturnImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				1
		);

		switch ( list.size() ) {
			case 0:
				return null;
			case 1:
				//noinspection unchecked
				return (T) list.get( 0 );
		}
		throw new HibernateException(
				"More than one row with the given identifier was found: " +
						ukValue +
						", for class: " +
						entityDescriptor.getEntityName()
		);
	}

	@Override
	public Object resolveId(Object ukValue, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		// todo (6.0) : cache the SQL AST and JdbcParameters
		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelectByUniqueKey(
				entityDescriptor,
				singletonList( entityDescriptor.getIdentifierMapping() ),
				uniqueKeyAttribute,
				null,
				new LoadQueryInfluencers( sessionFactory ),
				LockOptions.NONE,
				jdbcParametersBuilder::add,
				sessionFactory
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		final JdbcParametersList jdbcParameters = jdbcParametersBuilder.build();
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				ukValue,
				uniqueKeyAttribute,
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();
		final JdbcOperationQuerySelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( jdbcParameterBindings, QueryOptions.NONE );

		final List<Object> list = sessionFactory.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new NoCallbackExecutionContext( session ),
				RowTransformerSingularReturnImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				1
		);

		assert list.size() == 1;

		return list.get( 0 );
	}

	private static class SingleUKEntityLoaderExecutionContext extends BaseExecutionContext {
		private final String uniqueKeyAttributePath;
		private final Object uniqueKey;
		private final Callback callback;
		private final QueryOptions queryOptions;

		public SingleUKEntityLoaderExecutionContext(
				String uniqueKeyAttributePath,
				Object uniqueKey,
				SharedSessionContractImplementor session,
				Boolean readOnly) {
			super( session );
			this.uniqueKeyAttributePath = uniqueKeyAttributePath;
			this.uniqueKey = uniqueKey;
			//Careful, readOnly is possibly null
			this.queryOptions = readOnly == null ? QueryOptions.NONE : readOnly ? QueryOptions.READ_ONLY : QueryOptions.READ_WRITE;
			callback = new CallbackImpl();
		}

		@Override
		public QueryOptions getQueryOptions() {
			return queryOptions;
		}

		@Override
		public Callback getCallback() {
			return callback;
		}

		@Override
		public String getEntityUniqueKeyAttributePath() {
			return uniqueKeyAttributePath;
		}

		@Override
		public Object getEntityUniqueKey() {
			return uniqueKey;
		}
	}

}
