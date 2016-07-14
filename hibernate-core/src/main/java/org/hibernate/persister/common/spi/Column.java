/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.spi;

import org.hibernate.persister.common.internal.DerivedColumn;
import org.hibernate.persister.common.internal.PhysicalColumn;

/**
 * Represents the commonality between {@link PhysicalColumn} and {@link DerivedColumn}
 *
 * @author Steve Ebersole
 */
public interface Column {
	Table getSourceTable();
	// todo : SqlTypeDescriptor would be better, along with nullable, etc information
	int getJdbcType();
	String toLoggableString();

	String render(String identificationVariable);
}
