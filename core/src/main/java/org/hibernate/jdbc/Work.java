/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.HibernateException;

/**
 * Contract for performing a discrete piece of JDBC work.
 *
 * @author Steve Ebersole
 */
public interface Work {
	/**
	 * Execute the discrete work encapsulated by this work instance using the supplied connection.
	 *
	 * @param connection The connection on which to perform the work.
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws HibernateException Generally indicates a wrapped SQLException.
	 */
	public void execute(Connection connection) throws SQLException;
}
