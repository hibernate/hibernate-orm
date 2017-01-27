/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.sql;

/**
 * Describes a JDBC/SQL type.
 *
 * @author Steve Ebersole
 */
public interface SqlTypeDescriptor {
	/**
	 * Retrieve the JDBC/SQL type-code that this descriptor represents.
	 * <p/>
	 * For a "standard" type that would match the corresponding value in
	 * {@link java.sql.Types}.
	 *
	 * @return typeCode The JDBC/SQL type-code
	 */
	int getSqlType();
}
