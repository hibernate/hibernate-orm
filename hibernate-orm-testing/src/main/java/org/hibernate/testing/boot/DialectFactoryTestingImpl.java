/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.boot;

import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;

import org.hibernate.testing.env.ConnectionProviderBuilder;

/**
 * @author Steve Ebersole
 */
public class DialectFactoryTestingImpl implements DialectFactory {
	private final Dialect dialect;

	public DialectFactoryTestingImpl() {
		this( ConnectionProviderBuilder.getCorrespondingDialect() );
	}

	public DialectFactoryTestingImpl(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public Dialect buildDialect(Map configValues, DialectResolutionInfoSource resolutionInfoSource) {
		return dialect;
	}
}
