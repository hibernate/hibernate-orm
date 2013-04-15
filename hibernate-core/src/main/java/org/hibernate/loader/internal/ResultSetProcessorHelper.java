/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.spi.NamedParameterContext;
import org.hibernate.loader.spi.ResultSetProcessingContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class ResultSetProcessorHelper {
	public static EntityKey getOptionalObjectKey(QueryParameters queryParameters, SessionImplementor session) {
		final Object optionalObject = queryParameters.getOptionalObject();
		final Serializable optionalId = queryParameters.getOptionalId();
		final String optionalEntityName = queryParameters.getOptionalEntityName();

		if ( optionalObject != null && optionalEntityName != null ) {
			return session.generateEntityKey( optionalId, session.getEntityPersister( optionalEntityName, optionalObject ) );
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
