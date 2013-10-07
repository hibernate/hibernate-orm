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

import org.hibernate.dialect.Dialect;
import org.hibernate.service.Service;

/**
 * Contract for determining the {@link Dialect} to use based on information about the database / driver.
 *
 * @author Tomoto Shimizu Washio
 * @author Steve Ebersole
 */
public interface DialectResolver extends Service {
	/**
	 * Determine the {@link Dialect} to use based on the given information.  Implementations are expected to return
	 * the {@link Dialect} instance to use, or {@code null} if the they did not locate a match.
	 *
	 * @param info Access to the information about the database/driver needed to perform the resolution
	 *
	 * @return The dialect to use, or null.
	 */
	public Dialect resolveDialect(DialectResolutionInfo info);
}
