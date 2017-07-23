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

	// todo (6.0) - is this needed?
	//		currently its only usage is for org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor
	//		to extend it.  Perhaps we planned/intended to expose this as an API
	//		somewhere?

	/**
	 * Retrieve the JDBC/SQL type-code that this descriptor represents.
	 * <p/>
	 * For a "standard" type that would match the corresponding value in
	 * {@link java.sql.Types}.
	 *
	 * @return typeCode The JDBC/SQL type-code
	 */
	int getJdbcTypeCode();
}
