/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.loader.spi.ResultSetProcessingContext;

/**
 * Represents a return value in the query results.  Not the same as a result (column) in the JDBC ResultSet!
 * <p/>
 * Return is distinctly different from a {@link Fetch} and so modeled as completely separate hierarchy.
 *
 * @see ScalarReturn
 * @see EntityReturn
 * @see CollectionReturn
 *
 * @author Steve Ebersole
 */
public interface Return {
	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException;

	/**
	 * Effectively performs first phase of two-phase loading.  For scalar results first/second phase is one.  For
	 * entities, first phase is to resolve identifiers; second phase is to resolve the entity instances.
	 *
	 * @param resultSet The result set being processed
	 * @param context The context for the processing
	 *
	 * @throws SQLException Indicates a problem access the JDBC result set
	 */
	public void resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException;

	/**
	 * Essentially performs the second phase of two-phase loading.
	 *
	 * @param resultSet The result set being processed
	 * @param context The context for the processing
	 *
	 * @return The read object
	 *
	 * @throws SQLException Indicates a problem access the JDBC result set
	 */
	public Object read(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException;
}
