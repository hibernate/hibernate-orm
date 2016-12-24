/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.spi;

/**
 * Represents the commonality between {@link PhysicalColumn} and {@link DerivedColumn}
 *
 * @author Steve Ebersole
 */
public interface Column {
	Table getSourceTable();

	String getExpression();

	// todo : SqlTypeDescriptor would be better
	int getJdbcType();

	// todo : nullable, size, etc

	String toLoggableString();

	String render(String identificationVariable);
}
