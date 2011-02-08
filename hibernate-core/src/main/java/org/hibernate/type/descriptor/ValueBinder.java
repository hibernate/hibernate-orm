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
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Contract for binding values to a {@link PreparedStatement}.
 *
 * @author Steve Ebersole
 */
public interface ValueBinder<X> {
	/**
	 * Bind a value to a prepared statement.
	 *
	 * @param st The prepared statement to which to bind the value.
	 * @param value The value to bind.
	 * @param index The position at which to bind the value within the prepared statement
	 * @param options The options.
	 *
	 * @throws SQLException Indicates a JDBC error occurred.
	 */
	public void bind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException;
}
