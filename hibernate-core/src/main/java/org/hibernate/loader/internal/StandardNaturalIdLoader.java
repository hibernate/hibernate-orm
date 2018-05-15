/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.NaturalIdLoader;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.consume.spi.StandardParameterBindingContext;
import org.hibernate.sql.ast.produce.metamodel.internal.SelectByNaturalIdBuilder;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class StandardNaturalIdLoader implements NaturalIdLoader {
	private static final Logger log = Logger.getLogger( StandardNaturalIdLoader.class );

	private final EntityTypeDescriptor entityDescriptor;

	private Boolean simple;
	private JdbcSelect xrefSelect;

	public StandardNaturalIdLoader(EntityTypeDescriptor entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public Object resolveNaturalIdToEntity(
			Object naturalIdToLoad,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		if ( naturalIdToLoad == null ) {
			throw new HibernateException( "Natural-id to load cannot be null" );
		}

		if ( xrefSelect == null ) {
			xrefSelect = generatePkByNaturalIdSelect();
		}


		final JdbcParameterBindingsImpl jdbcParameterBindings = new JdbcParameterBindingsImpl();

		final List<NaturalIdDescriptor.NaturalIdAttributeInfo> naturalIdAttributeInfoList = entityDescriptor.getHierarchy()
				.getNaturalIdDescriptor()
				.getAttributeInfos();

		this.simple = naturalIdAttributeInfoList.size() == 1;

		if ( simple ) {
			if ( naturalIdToLoad instanceof Object[] ) {
				naturalIdToLoad = ( (Object[]) naturalIdToLoad )[0];

			}

			naturalIdAttributeInfoList.get( 0 ).getUnderlyingAttributeDescriptor().dehydrate(
					naturalIdToLoad,
					(jdbcValue, type, boundColumn) -> {
						jdbcParameterBindings.addBinding(
								new StandardJdbcParameterImpl(
										jdbcParameterBindings.getBindings().size(),
										type,
										Clause.WHERE,
										session.getFactory().getTypeConfiguration()
								),
								new JdbcParameterBinding() {
									@Override
									public SqlExpressableType getBindType() {
										return type;
									}

									@Override
									public Object getBindValue() {
										return jdbcValue;
									}
								}
						);
					},
					Clause.WHERE,
					session
			);
		}
		else {
			assert naturalIdToLoad instanceof Object[];
			final Object[] naturalIdValues = (Object[]) naturalIdToLoad;

			entityDescriptor.getHierarchy().getNaturalIdDescriptor().visitPersistentAttributes(
					new Consumer<NaturalIdDescriptor.NaturalIdAttributeInfo>() {
						@Override
						public void accept(NaturalIdDescriptor.NaturalIdAttributeInfo naturalIdAttributeInfo) {
							final Object idValue = naturalIdValues[ naturalIdAttributeInfo.getStateArrayPosition() ];
							naturalIdAttributeInfo.getUnderlyingAttributeDescriptor().dehydrate(
									idValue,
									(jdbcValue, type, boundColumn) -> {
										jdbcParameterBindings.addBinding(
												new StandardJdbcParameterImpl(
														jdbcParameterBindings.getBindings().size(),
														type,
														Clause.WHERE,
														session.getFactory().getTypeConfiguration()
												),
												new JdbcParameterBinding() {
													@Override
													public SqlExpressableType getBindType() {
														return type;
													}

													@Override
													public Object getBindValue() {
														return jdbcValue;
													}
												}
										);
									},
									Clause.WHERE,
									session
							);
						}
					}
			);
		}

		final ParameterBindingContext parameterBindingContext = new StandardParameterBindingContext(
				session.getFactory(),
				QueryParameterBindings.NO_PARAM_BINDINGS,
				Collections.emptyList()
		);

		final ExecutionContext executionContext = new ExecutionContext() {

			@Override
			public SharedSessionContractImplementor getSession() {
				return session;
			}

			@Override
			public QueryOptions getQueryOptions() {
				return QueryOptions.NONE;
			}

			@Override
			public ParameterBindingContext getParameterBindingContext() {
				return parameterBindingContext;
			}

			@Override
			public JdbcParameterBindings getJdbcParameterBindings() {
				return jdbcParameterBindings;
			}

			@Override
			public Callback getCallback() {
				return afterLoadAction -> {};
			}
		};

		final List list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
				xrefSelect,
				executionContext,
				RowTransformerSingularReturnImpl.instance()
		);

		if ( list.isEmpty() ) {
			return null;
		}

		return list.get( 0 );
	}

	private JdbcSelect generatePkByNaturalIdSelect() {
		final SelectByNaturalIdBuilder selectBuilder = new SelectByNaturalIdBuilder(
				entityDescriptor.getFactory(),
				entityDescriptor
		);
		final SqlAstSelectDescriptor selectDescriptor = selectBuilder
				.generateSelectStatement( 1, LoadQueryInfluencers.NONE, LockOptions.NONE );


		return SqlAstSelectToJdbcSelectConverter.interpret(
				selectDescriptor,
				entityDescriptor.getFactory()
		);
	}

	@Override
	public Object load(Object naturalIdToLoad, LoadOptions options, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public EntityTypeDescriptor getLoadedNavigable() {
		return entityDescriptor;
	}

//		if ( log.isTraceEnabled() ) {
//			log.tracef(
//					"Resolving natural-id [%s] to id : %s ",
//					naturalIdValues,
//					MessageHelper.infoString( this )
//			);
//		}
//
//		final boolean[] valueNullness = determineValueNullness( naturalIdValues );
//		final String sqlEntityIdByNaturalIdString = determinePkByNaturalIdQuery( valueNullness );
//
//		try {
//			PreparedStatement ps = session
//					.getJdbcCoordinator()
//					.getStatementPreparer()
//					.prepareStatement( sqlEntityIdByNaturalIdString );
//			try {
//				int positions = 1;
//				int loop = 0;
//				for ( int idPosition : getNaturalIdentifierProperties() ) {
//					final Object naturalIdValue = naturalIdValues[loop++];
//					if ( naturalIdValue != null ) {
//						final Type type = getPropertyTypes()[idPosition];
//						type.nullSafeSet( ps, naturalIdValue, positions, session );
//						positions += type.getColumnSpan( session.getFactory() );
//					}
//				}
//				ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( ps );
//				try {
//					// if there is no resulting row, return null
//					if ( !rs.next() ) {
//						return null;
//					}
//
//					final Object hydratedId = getIdentifierType().hydrate( rs, getIdentifierAliases(), session, null );
//					return (Serializable) getIdentifierType().resolve( hydratedId, session, null );
//				}
//				finally {
//					session.getJdbcCoordinator().getResourceRegistry().release( rs, ps );
//				}
//			}
//			finally {
//				session.getJdbcCoordinator().getResourceRegistry().release( ps );
//				session.getJdbcCoordinator().afterStatementExecution();
//			}
//		}
//		catch (SQLException e) {
//			throw getFactory().getSQLExceptionHelper().convert(
//					e,
//					String.format(
//							"could not resolve natural-id [%s] to id : %s",
//							naturalIdValues,
//							MessageHelper.infoString( this )
//					),
//					sqlEntityIdByNaturalIdString
//			);
//		}
//	}





//	private boolean[] determineValueNullness(Object[] naturalIdValues) {
//		boolean[] nullness = new boolean[naturalIdValues.length];
//		for ( int i = 0; i < naturalIdValues.length; i++ ) {
//			nullness[i] = naturalIdValues[i] == null;
//		}
//		return nullness;
//	}
//
//	private Boolean naturalIdIsNonNullable;
//	private String cachedPkByNonNullableNaturalIdQuery;
//
//	private String determinePkByNaturalIdQuery(boolean[] valueNullness) {
//		if ( !hasNaturalIdentifier() ) {
//			throw new HibernateException(
//					"Attempt to build natural-id -> PK resolution query for entity that does not define natural id"
//			);
//		}
//
//		// performance shortcut for cases where the natural-id is defined as completely non-nullable
//		if ( isNaturalIdNonNullable() ) {
//			if ( valueNullness != null && !ArrayHelper.isAllFalse( valueNullness ) ) {
//				throw new HibernateException( "Null value(s) passed to lookup by non-nullable natural-id" );
//			}
//			if ( cachedPkByNonNullableNaturalIdQuery == null ) {
//				cachedPkByNonNullableNaturalIdQuery = generateEntityIdByNaturalIdSql( null );
//			}
//			return cachedPkByNonNullableNaturalIdQuery;
//		}
//
//		// Otherwise, regenerate it each time
//		return generateEntityIdByNaturalIdSql( valueNullness );
//	}
//
//	protected boolean isNaturalIdNonNullable() {
//		if ( naturalIdIsNonNullable == null ) {
//			naturalIdIsNonNullable = determineNaturalIdNullability();
//		}
//		return naturalIdIsNonNullable;
//	}
//
//	private boolean determineNaturalIdNullability() {
//		boolean[] nullability = getPropertyNullability();
//		for ( int position : getNaturalIdentifierProperties() ) {
//			// if any individual property is nullable, return false
//			if ( nullability[position] ) {
//				return false;
//			}
//		}
//		// return true if we found no individually nullable properties
//		return true;
//	}
//
//	private String generateEntityIdByNaturalIdSql(boolean[] valueNullness) {
//		EntityPersister rootPersister = getFactory().getEntityDescriptor( getRootEntityName() );
//		if ( rootPersister != this ) {
//			if ( rootPersister instanceof AbstractEntityPersister ) {
//				return ( (AbstractEntityPersister) rootPersister ).generateEntityIdByNaturalIdSql( valueNullness );
//			}
//		}
//
//		Select select = new Select( getFactory().getDialect() );
//		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
//			select.setComment( "get current natural-id->entity-id state " + getEntityName() );
//		}
//
//		final String rootAlias = getRootAlias();
//
//		select.setSelectClause( identifierSelectFragment( rootAlias, "" ) );
//		select.setFromClause( fromTableFragment( rootAlias ) + fromJoinFragment( rootAlias, true, false ) );
//
//		final StringBuilder whereClause = new StringBuilder();
//		final int[] propertyTableNumbers = getPropertyTableNumbers();
//		final int[] naturalIdPropertyIndexes = this.getNaturalIdentifierProperties();
//		int valuesIndex = -1;
//		for ( int propIdx = 0; propIdx < naturalIdPropertyIndexes.length; propIdx++ ) {
//			valuesIndex++;
//			if ( propIdx > 0 ) {
//				whereClause.append( " and " );
//			}
//
//			final int naturalIdIdx = naturalIdPropertyIndexes[propIdx];
//			final String tableAlias = generateTableAlias( rootAlias, propertyTableNumbers[naturalIdIdx] );
//			final String[] propertyColumnNames = getPropertyColumnNames( naturalIdIdx );
//			final String[] aliasedPropertyColumns = StringHelper.qualify( tableAlias, propertyColumnNames );
//
//			if ( valueNullness != null && valueNullness[valuesIndex] ) {
//				whereClause.append( StringHelper.join( " is null and ", aliasedPropertyColumns ) ).append( " is null" );
//			}
//			else {
//				whereClause.append( StringHelper.join( "=? and ", aliasedPropertyColumns ) ).append( "=?" );
//			}
//		}
//
//		whereClause.append( whereJoinFragment( getRootAlias(), true, false ) );
//
//		return select.setOuterJoins( "", "" ).setWhereClause( whereClause.toString() ).toStatementString();
//	}
}
