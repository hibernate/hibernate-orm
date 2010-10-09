/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.exception;

import java.sql.SQLException;

/**
 * Defines a contract for implementations that can extract the name of a violated
 * constraint from a SQLException that is the result of that constraint violation.
 *
 * @author Steve Ebersole
 */
public interface ViolatedConstraintNameExtracter {
	/**
	 * Extract the name of the violated constraint from the given SQLException.
	 *
	 * @param sqle The exception that was the result of the constraint violation.
	 * @return The extracted constraint name.
	 */
	public String extractConstraintName(SQLException sqle);
}
