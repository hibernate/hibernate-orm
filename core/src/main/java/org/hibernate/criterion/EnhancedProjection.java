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
package org.hibernate.criterion;


import java.io.Serializable;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

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
	 * Hibernate to be able to extract these val;ues correctly.
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
