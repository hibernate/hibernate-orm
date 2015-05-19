/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Contract describing source of table information
 *
 * @author Steve Ebersole
 */
public interface TableSource extends TableSpecificationSource {
	/**
	 * Obtain the supplied table name.
	 *
	 * @return The table name, or {@code null} is no name specified.
	 */
	String getExplicitTableName();

	String getRowId();

	String getComment();

	String getCheckConstraint();
}
