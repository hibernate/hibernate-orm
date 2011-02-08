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
package org.hibernate.loader.custom;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extension point allowing any SQL query with named and positional parameters
 * to be executed by Hibernate, returning managed entities, collections and
 * simple scalar values.
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface CustomQuery {
	/**
	 * The SQL query string to be performed.
	 *
	 * @return The SQL statement string.
	 */
	public String getSQL();

	/**
	 * Any query spaces to apply to the query execution.  Query spaces are
	 * used in Hibernate's auto-flushing mechanism to determine which
	 * entities need to be checked for pending changes.
	 *
	 * @return The query spaces
	 */
	public Set getQuerySpaces();

	/**
	 * A map representing positions within the supplied {@link #getSQL query} to
	 * which we need to bind named parameters.
	 * <p/>
	 * Optional, may return null if no named parameters.
	 * <p/>
	 * The structure of the returned map (if one) as follows:<ol>
	 * <li>The keys into the map are the named parameter names</li>
	 * <li>The corresponding value is either an {@link Integer} if the
	 * parameter occurs only once in the query; or a List of Integers if the
	 * parameter occurs more than once</li>
	 * </ol>
	 */
	public Map getNamedParameterBindPoints();

	/**
	 * A collection of {@link Return descriptors} describing the
	 * JDBC result set to be expected and how to map this result set.
	 *
	 * @return List of return descriptors.
	 */
	public List getCustomQueryReturns();
}
