/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.SimpleNaturalIdLoader;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardSimpleNaturalIdLoaderImpl implements SimpleNaturalIdLoader {
	private final EntityDescriptor entityDescriptor;

	public StandardSimpleNaturalIdLoaderImpl(EntityDescriptor entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public Object resolveIdentifier(Object naturalIdValue, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Object loadEntity(Object naturalIdValue, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();

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
	}





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
