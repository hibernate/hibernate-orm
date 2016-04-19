/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.param;


/**
 * An additional contract for parameters which originate from parameters explicitly encountered in the source statement
 * (HQL or native-SQL).
 *
 * @author Steve Ebersole
 */
public interface ExplicitParameterSpecification extends ParameterSpecification {
	/**
	 * Retrieves the line number on which this parameter occurs in the source query.
	 *
	 * @return The line number.
	 */
	int getSourceLine();

	/**
	 * Retrieves the column number (within the {@link #getSourceLine()}) where this parameter occurs.
	 *
	 * @return The column number.
	 */
	int getSourceColumn();
}
