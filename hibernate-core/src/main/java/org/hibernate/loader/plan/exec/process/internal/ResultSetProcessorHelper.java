/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class ResultSetProcessorHelper {
	/**
	 * Singleton access
	 */
	public static final ResultSetProcessorHelper INSTANCE = new ResultSetProcessorHelper();

	public static EntityKey getOptionalObjectKey(QueryParameters queryParameters, SessionImplementor session) {
		final Object optionalObject = queryParameters.getOptionalObject();
		final Serializable optionalId = queryParameters.getOptionalId();
		final String optionalEntityName = queryParameters.getOptionalEntityName();

		return INSTANCE.interpretEntityKey( session, optionalEntityName, optionalId, optionalObject );
	}

	public EntityKey interpretEntityKey(
			SessionImplementor session,
			String optionalEntityName,
			Serializable optionalId,
			Object optionalObject) {
		if ( optionalEntityName != null ) {
			final EntityPersister entityPersister;
			if ( optionalObject != null ) {
				entityPersister = session.getEntityPersister( optionalEntityName, optionalObject );
			}
			else {
				entityPersister = session.getFactory().getEntityPersister( optionalEntityName );
			}
			if ( entityPersister.isInstance( optionalId ) &&
					!entityPersister.getEntityMetamodel().getIdentifierProperty().isVirtual() &&
					entityPersister.getEntityMetamodel().getIdentifierProperty().isEmbedded() ) {
				// non-encapsulated composite identifier
				final Serializable identifierState = ((CompositeType) entityPersister.getIdentifierType()).getPropertyValues(
						optionalId,
						session
				);
				return session.generateEntityKey( identifierState, entityPersister );
			}
			else {
				return session.generateEntityKey( optionalId, entityPersister );
			}
		}
		else {
			return null;
		}
	}

	public static Map<String, int[]> buildNamedParameterLocMap(
			QueryParameters queryParameters,
			NamedParameterContext namedParameterContext) {
		if ( queryParameters.getNamedParameters() == null || queryParameters.getNamedParameters().isEmpty() ) {
			return null;
		}

		final Map<String, int[]> namedParameterLocMap = new HashMap<String, int[]>();
		for ( String name : queryParameters.getNamedParameters().keySet() ) {
			namedParameterLocMap.put(
					name,
					namedParameterContext.getNamedParameterLocations( name )
			);
		}
		return namedParameterLocMap;
	}

}
