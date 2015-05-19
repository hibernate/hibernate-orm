/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.spi;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.Service;

/**
 * A factory for generating Dialect instances.
 *
 * @author Steve Ebersole
 */
public interface DialectFactory extends Service {
	/**
	 * Builds an appropriate Dialect instance.
	 * <p/>
	 * If a dialect is explicitly named in the incoming properties, it should used. Otherwise, it is
	 * determined by dialect resolvers based on the passed connection.
	 * <p/>
	 * An exception is thrown if a dialect was not explicitly set and no resolver could make
	 * the determination from the given connection.
	 *
	 * @param configValues The configuration properties.
	 * @param resolutionInfoSource Access to DialectResolutionInfo used to resolve the Dialect to use if not
	 * explicitly named
	 *
	 * @return The appropriate dialect instance.
	 *
	 * @throws HibernateException No dialect specified and no resolver could make the determination.
	 */
	public Dialect buildDialect(Map configValues, DialectResolutionInfoSource resolutionInfoSource) throws HibernateException;
}
