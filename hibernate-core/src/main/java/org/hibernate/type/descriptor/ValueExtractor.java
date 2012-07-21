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
package org.hibernate.type.descriptor;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Contract for extracting value via JDBC (from {@link ResultSet} or as output param from {@link CallableStatement}).
 *
 * @author Steve Ebersole
 */
public interface ValueExtractor<X> {
	/**
	 * Extract value from result set
	 *
	 * @param rs The result set from which to extract the value
	 * @param name The name by which to extract the value from the result set
	 * @param options The options
	 *
	 * @return The extracted value
	 *
	 * @throws SQLException Indicates a JDBC error occurred.
	 */
	public X extract(ResultSet rs, String name, WrapperOptions options) throws SQLException;

	public X extract(CallableStatement rs, int index, WrapperOptions options) throws SQLException;

	public X extract(CallableStatement statement, String[] paramNames, WrapperOptions options) throws SQLException;
}
