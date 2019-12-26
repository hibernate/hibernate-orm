/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;

/**
 * An "enhanced" Projection for a {@link Criteria} query.
 *
 * @author Gail Badner
 * @see Projection
 * @see Criteria
 */
public interface EnhancedProjection extends Projection {

	/**
	 * Get the SQL column aliases used by this projection for the columns it writes for inclusion into the
	 * <tt>SELECT</tt> clause ({@link #toSqlString}.  Hibernate always uses column aliases to extract data from the
	 * JDBC {@link java.sql.ResultSet}, so it is important that these be implemented correctly in order for
	 * Hibernate to be able to extract these values correctly.
	 *
	 * @param position Just as in {@link #toSqlString}, represents the number of <b>columns</b> rendered
	 * prior to this projection.
	 * @param criteria The local criteria to which this project is attached (for resolution).
	 * @param criteriaQuery The overall criteria query instance.
	 * @return The columns aliases.
	 */
	public String[] getColumnAliases(int position, Criteria criteria, CriteriaQuery criteriaQuery);

	/**
	 * Get the SQL column aliases used by this projection for the columns it writes for inclusion into the
	 * <tt>SELECT</tt> clause ({@link #toSqlString} <i>for a particular criteria-level alias</i>.
	 *
	 * @param alias The criteria-level alias
	 * @param position Just as in {@link #toSqlString}, represents the number of <b>columns</b> rendered
	 * prior to this projection.
	 * @param criteria The local criteria to which this project is attached (for resolution).
	 * @param criteriaQuery The overall criteria query instance.
	 * @return The columns aliases pertaining to a particular criteria-level alias; expected to return null if
	 * this projection does not understand this alias.
	 */
	public String[] getColumnAliases(String alias, int position, Criteria criteria, CriteriaQuery criteriaQuery);
}
