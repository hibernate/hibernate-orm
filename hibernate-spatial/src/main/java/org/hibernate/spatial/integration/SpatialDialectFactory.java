/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.spatial.integration;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.spatial.HibernateSpatialConfiguration;
import org.hibernate.spatial.dialect.oracle.OracleSpatial10gDialect;

/**
 * A {@code DialectFactory} that may inject configuration into {@code SpatialDialect}s.
 *
 * This implementation extends the Standard Hibernate {@code DialectFactory}. It is currently only used
 * for special handling of the {@code OracleSpatial10gDialect}.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/23/13
 */
public class SpatialDialectFactory extends DialectFactoryImpl {
	private final HibernateSpatialConfiguration spatialConfig;

	/**
	 * Constructs an instance with the specified configuration
	 *
	 * @param spatialConfig the spatial configuration to use.
	 */
	public SpatialDialectFactory(HibernateSpatialConfiguration spatialConfig) {
		super();
		this.spatialConfig = spatialConfig;
	}

	@Override
	public Dialect buildDialect(Map configValues, DialectResolutionInfoSource resolutionInfoSource) throws HibernateException {
		final Dialect dialect = super.buildDialect( configValues, resolutionInfoSource );
		if (dialect instanceof OracleSpatial10gDialect) {
			return new OracleSpatial10gDialect( spatialConfig );
		}
		else {
			return dialect;
		}
	}
}
