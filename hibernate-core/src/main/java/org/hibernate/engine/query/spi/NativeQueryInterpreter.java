/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.query.spi;

import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.Service;

/**
 * Service contract for dealing with native queries.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
public interface NativeQueryInterpreter extends Service {
	/**
	 * Returns a meta-data object with information about the named and ordinal
	 * parameters contained in the given native query.
	 *
	 * @param nativeQuery the native query to analyze.
	 *
	 * @return a meta-data object describing the parameters of the given query.
	 *         Must not be {@code null}.
	 */
	ParameterMetadata getParameterMetadata(String nativeQuery);

	/**
	 * Creates a new query plan for the specified native query.
	 *
	 * @param specification Describes the query to create a plan for
	 * @param sessionFactory The current session factory
	 *
	 * @return A query plan for the specified native query.
	 */
	NativeSQLQueryPlan createQueryPlan(NativeSQLQuerySpecification specification, SessionFactoryImplementor sessionFactory);
}
