/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ordering.antlr;

import org.hibernate.HibernateException;

/**
 * Contract for mapping a (an assumed) property reference to its columns.
 *
 * @author Steve Ebersole
 */
public interface ColumnMapper {
	/**
	 * Resolve the property reference to its underlying columns.
	 *
	 * @param reference The property reference name.
	 *
	 * @return References to the columns/formulas that define the value mapping for the given property, or null
	 * if the property reference is unknown.
	 *
	 * @throws HibernateException Generally indicates that the property reference is unknown; interpretation
	 * should be the same as a null return.
	 */
	public SqlValueReference[] map(String reference) throws HibernateException;
}
