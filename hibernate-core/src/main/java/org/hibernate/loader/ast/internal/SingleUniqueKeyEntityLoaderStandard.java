package org.hibernate.loader.ast.internal;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * @author Steve Ebersole
 */
public class SingleUniqueKeyEntityLoaderStandard<T> implements SingleUniqueKeyEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	private final ModelPart uniqueKeyAttribute;
	@Nonnull
	private final String uniqueKeyAttributePath;
	private final JdbcParametersList jdbcParameters;
	private final JdbcSelect jdbcSelect;

	public SingleUniqueKeyEntityLoaderStandard(
			@Nonnull EntityMappingType entityDescriptor,
			@Nonnull SingularAttributeMapping uniqueKeyMapping,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers) {
		this.entityDescriptor = entityDescriptor;
		uniqueKeyAttributePath = getAttributePath( uniqueKeyMapping );
		uniqueKeyAttribute =
				uniqueKeyMapping instanceof ToOneAttributeMapping toOneAttributeMapping
						? toOneAttributeMapping.getForeignKeyDescriptor()
						: uniqueKeyMapping;

		final var factory = entityDescriptor.getEntityPersister().getFactory();
		final var builder = JdbcParametersList.newBuilder();
		final var sqlAst = LoaderSelectBuilder.createSelectByUniqueKey(
				entityDescriptor,
				emptyList(),
				uniqueKeyMapping,
				null,
				loadQueryInfluencers,
				new LockOptions(),
				builder::add,
				new SqlAliasBaseManager(),
				factory
		);
		jdbcParameters = builder.build();
		jdbcSelect = getJdbcSelect( factory, sqlAst, JdbcParameterBindings.NO_BINDINGS );
	}

	@Nonnull
	private static String getAttributePath(@Nonnull AttributeMapping attribute) {
		ManagedMappingType declaringType = attribute.getDeclaringType();
		if ( declaringType instanceof EmbeddableMappingType ) {
			final var path = new StringBuilder();
			path.append( castNonNull( attribute.getAttributeName() ) );
			do {
				// declaringType must be cast each time (not a pattern variable) as it's updated each iteration
				final var valueMapping = ( (EmbeddableMappingType) declaringType ).getEmbeddedValueMapping();
				attribute = valueMapping.asAttributeMapping();
				if ( attribute == null ) {
					break;
				}
				path.insert( 0, '.' );
				path.insert( 0, castNonNull( attribute.getAttributeName() ) );
				declaringType = attribute.getDeclaringType();
			} while ( declaringType instanceof EmbeddableMappingType );
			return path.toString();
		}
		return castNonNull( attribute.getAttributeName() );
	}

	@Nonnull
	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Nullable
	@Override
	public T load(
			@Nonnull Object ukValue,
			@Nonnull LockOptions lockOptions,
			@Nullable Boolean readOnly,
			@Nonnull SharedSessionContractImplementor session) {
		final var bindings = jdbcParameterBindings( ukValue, jdbcParameters, session );
		final List<T> list = list( jdbcSelect, bindings,
				new SingleUKEntityLoaderExecutionContext( uniqueKeyAttributePath, ukValue, session, readOnly ) );
		return switch ( list.size() ) {
			case 0 -> null;
			case 1 -> list.get( 0 );
			default -> throw new HibernateException( "More than one row with the given identifier was found: "
								+ ukValue + ", for class: " + entityDescriptor.getEntityName() );
		};
	}

	@Nullable
	@Override
	public Object resolveId(@Nonnull Object ukValue, @Nonnull SharedSessionContractImplementor session) {
		final var factory = session.getFactory();
		// todo (6.0) : cache the SQL AST and JdbcParameters
		final var builder = JdbcParametersList.newBuilder();
		final var sqlAst = LoaderSelectBuilder.createSelectByUniqueKey(
				entityDescriptor,
				singletonList( entityDescriptor.getIdentifierMapping() ),
				uniqueKeyAttribute,
				null,
				new LoadQueryInfluencers( factory ),
				new LockOptions(),
				builder::add,
				new SqlAliasBaseManager(),
				factory
		);
		final var bindings = jdbcParameterBindings( ukValue, builder.build(), session );
		final var jdbcSelect = getJdbcSelect( factory, sqlAst, bindings );
		final List<Object> list = list( jdbcSelect, bindings, new NoCallbackExecutionContext( session ) );
		assert list.size() == 1;
		return list.get( 0 );
	}

	@Nonnull
	private JdbcParameterBindings jdbcParameterBindings(
			@Nonnull Object ukValue,
			@Nonnull JdbcParametersList parameters,
			@Nonnull SharedSessionContractImplementor session) {
		final var bindings = new JdbcParameterBindingsImpl( parameters.size() );
		final int offset = bindings.registerParametersForEachJdbcValue( ukValue, uniqueKeyAttribute, parameters, session );
		assert offset == parameters.size();
		return bindings;
	}

	@Nonnull
	private static <T> List<T> list(
			@Nonnull JdbcSelect jdbcSelect,
			@Nonnull JdbcParameterBindings jdbcParameterBindings,
			@Nonnull ExecutionContext executionContext) {
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

	@Nonnull
	private static JdbcSelect getJdbcSelect
			(@Nonnull SessionFactoryImplementor factory, @Nonnull SelectStatement sqlAst, @Nonnull JdbcParameterBindings jdbcParameterBindings) {
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.Select( factory, sqlAst ) )
				.translate( jdbcParameterBindings, QueryOptions.NONE );
	}

	private static class SingleUKEntityLoaderExecutionContext extends BaseExecutionContext {
		private final String uniqueKeyAttributePath;
		private final Object uniqueKey;
		private final Callback callback;
		private final QueryOptions queryOptions;

		public SingleUKEntityLoaderExecutionContext(
				@Nonnull String uniqueKeyAttributePath,
				@Nonnull Object uniqueKey,
				@Nonnull SharedSessionContractImplementor session,
				@Nullable Boolean readOnly) {
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

		@Nonnull
		@Override
		public QueryOptions getQueryOptions() {
			return queryOptions;
		}

		@Nonnull
		@Override
		public Callback getCallback() {
			return callback;
		}

		@Nonnull
		@Override
		public String getEntityUniqueKeyAttributePath() {
			return uniqueKeyAttributePath;
		}

		@Nonnull
		@Override
		public Object getEntityUniqueKey() {
			return uniqueKey;
		}
	}

}
