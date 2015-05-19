/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
