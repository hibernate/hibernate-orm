/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql;

import org.hibernate.type.Type;

/**
 * Describes a scalar query result.
 *
 * Defined via any of:
 *
 * 		* `<return-scalar/>` in `hbm.xml`
 * 	    * {@link javax.persistence.ColumnResult}
 * 	    * `<column-result/>` in `orm.xml`
 *
 * @author Steve Ebersole
 */
public interface ScalarResultRegistration extends ReturnableResultRegistration {
	/**
	 * The name of the represented column in the ResultSet
	 */
	String getColumnName();

	/**
	 * The Hibernate Type indicating how to read the value
	 */
	Type getType();
}
