/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;

/**
 * Standard MultiNaturalIdLoader implementation
 */
public class MultiNaturalIdLoaderArrayParam<E> implements MultiNaturalIdLoader<E>, SqlArrayMultiKeyLoader {
	private final EntityMappingType entityDescriptor;
	private final Class<?> keyClass;
	private final Class<?> keyArrayClass;

	public MultiNaturalIdLoaderArrayParam(EntityMappingType entityDescriptor) {
		assert entityDescriptor.getNaturalIdMapping() instanceof SimpleNaturalIdMapping;

		this.entityDescriptor = entityDescriptor;
		this.keyClass = entityDescriptor.getNaturalIdMapping().getJavaType().getJavaTypeClass();
		this.keyArrayClass = createTypedArray( 0 ).getClass();
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	protected SimpleNaturalIdMapping getNaturalIdMapping()  {
		return (SimpleNaturalIdMapping) entityDescriptor.getNaturalIdMapping();
	}

	protected BasicAttributeMapping getNaturalIdAttribute()  {
		return (BasicAttributeMapping) getNaturalIdMapping().asAttributeMapping();
	}

	@Override
	public <K> List<E> multiLoad(K[] naturalIds, MultiNaturalIdLoadOptions loadOptions, SharedSessionContractImplementor session) {
		if ( naturalIds == null ) {
			throw new IllegalArgumentException( "`naturalIds` is null" );
		}

		if ( naturalIds.length == 0 ) {
			return Collections.emptyList();
		}

		if ( MultiKeyLoadLogging.MULTI_KEY_LOAD_TRACE_ENABLED ) {
			MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.tracef( "MultiNaturalIdLoaderArrayParam#multiLoadStarting - `%s`", entityDescriptor.getEntityName() );
		}

		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final boolean coerce = !sessionFactory.getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled();

		assert naturalIds.getClass().isArray();
		if ( !naturalIds.getClass().getComponentType().equals( keyClass ) ) {
			final K[] typedArray = createTypedArray( naturalIds.length );
			if ( !coerce ) {
				System.arraycopy( naturalIds, 0, typedArray, 0, naturalIds.length );
			}
			else {
				for ( int i = 0; i < naturalIds.length; i++ ) {
					//noinspection unchecked
					typedArray[i] = (K) getNaturalIdAttribute().getJavaType().coerce( naturalIds[i], session );
				}
			}
			naturalIds = typedArray;
		}

		final List<E> result = CollectionHelper.arrayList( naturalIds.length );
		final LockOptions lockOptions = (loadOptions.getLockOptions() == null)
				? new LockOptions( LockMode.NONE )
				: loadOptions.getLockOptions();

		final BasicTypeRegistry basicTypeRegistry = sessionFactory.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<?> arrayBasicType = basicTypeRegistry.getRegisteredType( keyArrayClass );
		final JdbcMapping arrayJdbcMapping = MultiKeyLoadHelper.resolveArrayJdbcMapping(
				arrayBasicType,
				getNaturalIdMapping().getSingleJdbcMapping(),
				keyArrayClass,
				sessionFactory
		);
		final JdbcParameter jdbcParameter = new JdbcParameterImpl( arrayJdbcMapping );
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(1);
		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new JdbcParameterBindingImpl( arrayJdbcMapping, naturalIds )
		);

		final SelectStatement sqlAst = LoaderSelectBuilder.createSelectBySingleArrayParameter(
				getLoadable(),
				getNaturalIdAttribute(),
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameter,
				sessionFactory
		);
		final JdbcOperationQuerySelect jdbcSelectOperation = sessionFactory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );

		final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				session.getPersistenceContext().getBatchFetchQueue(),
				sqlAst,
				Collections.singletonList( jdbcParameter ),
				jdbcParameterBindings
		);

		final List<E> jdbcResults = session.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelectOperation,
				jdbcParameterBindings,
				new ExecutionContextWithSubselectFetchHandler( session, subSelectFetchableKeysHandler ),
				RowTransformerStandardImpl.instance(),
				ListResultsConsumer.UniqueSemantic.FILTER
		);
		result.addAll( jdbcResults );

		return result;
	}

	private <X> X[] createTypedArray(@SuppressWarnings("SameParameterValue") int length) {
		//noinspection unchecked
		return (X[]) Array.newInstance( getNaturalIdMapping().getJavaType().getJavaTypeClass(), length );
	}
}
