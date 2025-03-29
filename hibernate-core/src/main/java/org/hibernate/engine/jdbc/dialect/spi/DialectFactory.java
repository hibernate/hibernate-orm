/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.dialect.spi;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.Service;

/**
 * A factory for generating {@link Dialect} instances.
 *
 * @author Steve Ebersole
 */
public interface DialectFactory extends Service {
	/**
	 * Builds an appropriate Dialect instance.
	 * <p>
	 * If a dialect is explicitly named in the incoming properties, it should used. Otherwise, it is
	 * determined by dialect resolvers based on the passed connection.
	 * <p>
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
	Dialect buildDialect(Map<String,Object> configValues, DialectResolutionInfoSource resolutionInfoSource) throws HibernateException;
}
