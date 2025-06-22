/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
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
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static java.util.Collections.emptyList;
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
			SingularAttributeMapping uniqueKeyMapping,
			LoadQueryInfluencers loadQueryInfluencers) {
		this.entityDescriptor = entityDescriptor;
		uniqueKeyAttributePath = getAttributePath( uniqueKeyMapping );
		uniqueKeyAttribute =
				uniqueKeyMapping instanceof ToOneAttributeMapping toOneAttributeMapping
						? toOneAttributeMapping.getForeignKeyDescriptor()
						: uniqueKeyMapping;

		final SessionFactoryImplementor factory = entityDescriptor.getEntityPersister().getFactory();
		final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder();
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelectByUniqueKey(
				entityDescriptor,
				emptyList(),
				uniqueKeyMapping,
				null,
				loadQueryInfluencers,
				LockOptions.NONE,
				builder::add,
				factory
		);
		jdbcParameters = builder.build();
		jdbcSelect = getJdbcSelect( factory, sqlAst, JdbcParameterBindings.NO_BINDINGS );
	}

	private static String getAttributePath(AttributeMapping attribute) {
		ManagedMappingType declaringType = attribute.getDeclaringType();
		if ( declaringType instanceof EmbeddableMappingType ) {
			final StringBuilder path = new StringBuilder();
			path.append( attribute.getAttributeName() );
			do {
				final EmbeddableValuedModelPart embeddedValueMapping =
						( (EmbeddableMappingType) declaringType ).getEmbeddedValueMapping();
				attribute = embeddedValueMapping.asAttributeMapping();
				if ( attribute == null ) {
					break;
				}
				path.insert( 0, '.' );
				path.insert( 0, attribute.getAttributeName() );
				declaringType = attribute.getDeclaringType();
			} while ( declaringType instanceof EmbeddableMappingType );
			return path.toString();
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
		final JdbcParameterBindings bindings = jdbcParameterBindings( ukValue, jdbcParameters, session );
		final List<T> list = list( jdbcSelect, bindings,
				new SingleUKEntityLoaderExecutionContext( uniqueKeyAttributePath, ukValue, session, readOnly ) );
		return switch ( list.size() ) {
			case 0 -> null;
			case 1 -> list.get( 0 );
			default -> throw new HibernateException( "More than one row with the given identifier was found: "
								+ ukValue + ", for class: " + entityDescriptor.getEntityName() );
		};
	}

	@Override
	public Object resolveId(Object ukValue, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor factory = session.getFactory();
		// todo (6.0) : cache the SQL AST and JdbcParameters
		final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder();
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelectByUniqueKey(
				entityDescriptor,
				singletonList( entityDescriptor.getIdentifierMapping() ),
				uniqueKeyAttribute,
				null,
				new LoadQueryInfluencers( factory ),
				LockOptions.NONE,
				builder::add,
				factory
		);
		final JdbcParameterBindings bindings = jdbcParameterBindings( ukValue, builder.build(), session );
		final JdbcOperationQuerySelect jdbcSelect = getJdbcSelect( factory, sqlAst, bindings );
		final List<Object> list = list( jdbcSelect, bindings, new NoCallbackExecutionContext( session ) );
		assert list.size() == 1;
		return list.get( 0 );
	}

	private JdbcParameterBindings jdbcParameterBindings(
			Object ukValue,
			JdbcParametersList parameters,
			SharedSessionContractImplementor session) {
		final JdbcParameterBindings bindings = new JdbcParameterBindingsImpl( parameters.size() );
		final int offset = bindings.registerParametersForEachJdbcValue( ukValue, uniqueKeyAttribute, parameters, session );
		assert offset == parameters.size();
		return bindings;
	}

	private static <T> List<T> list(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		return executionContext.getSession().getJdbcServices().getJdbcSelectExecutor()
				.list(
						jdbcSelect,
						jdbcParameterBindings,
						executionContext,
						RowTransformerSingularReturnImpl.instance(),
						null,
						ListResultsConsumer.UniqueSemantic.FILTER,
						1
				);
	}

	private static JdbcOperationQuerySelect getJdbcSelect
			(SessionFactoryImplementor factory, SelectStatement sqlAst, JdbcParameterBindings jdbcParameterBindings) {
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildSelectTranslator( factory, sqlAst )
				.translate( jdbcParameterBindings, QueryOptions.NONE );
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
			if ( readOnly == null ) { //Careful, readOnly is possibly null
				queryOptions = QueryOptions.NONE;
			}
			else {
				queryOptions = readOnly ? QueryOptions.READ_ONLY : QueryOptions.READ_WRITE;
			}
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
