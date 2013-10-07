/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
