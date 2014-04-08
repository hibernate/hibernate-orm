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
 * This implementation extends the Standard Hibernate {@code DialectFactory}. It is currently only used for the
 * {@code OracleSpatial10gDialect}.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/23/13
 */
public class SpatialDialectFactory extends DialectFactoryImpl {

	private final HibernateSpatialConfiguration configuration;

	/**
	 * Constructs an instance with the specified configuration
	 * @param configuration the {@HibernateSpatialConfiguration} to use.
	 */
	public SpatialDialectFactory(HibernateSpatialConfiguration configuration) {
		super();
		this.configuration = configuration;
	}

	@Override
	public Dialect buildDialect(Map configValues, DialectResolutionInfoSource resolutionInfoSource) throws HibernateException {
		final Dialect dialect = super.buildDialect( configValues, resolutionInfoSource );
		if (dialect instanceof OracleSpatial10gDialect) {
			return new OracleSpatial10gDialect( configuration );
		}
		else {
			return dialect;
		}
	}
}
