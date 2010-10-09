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
package org.hibernate.engine.query;

import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Set;

/**
 * Defines metadata regarding a translated HQL or native-SQL query.
 *
 * @author Steve Ebersole
 */
public class QueryMetadata implements Serializable {
	private final String sourceQuery;
	private final ParameterMetadata parameterMetadata;
	private final String[] returnAliases;
	private final Type[] returnTypes;
	private final Set querySpaces;

	public QueryMetadata(
			String sourceQuery,
	        ParameterMetadata parameterMetadata,
	        String[] returnAliases,
	        Type[] returnTypes,
	        Set querySpaces) {
		this.sourceQuery = sourceQuery;
		this.parameterMetadata = parameterMetadata;
		this.returnAliases = returnAliases;
		this.returnTypes = returnTypes;
		this.querySpaces = querySpaces;
	}

	/**
	 * Get the source HQL or native-SQL query.
	 *
	 * @return The source query.
	 */
	public String getSourceQuery() {
		return sourceQuery;
	}

	public ParameterMetadata getParameterMetadata() {
		return parameterMetadata;
	}

	/**
	 * Return source query select clause aliases (if any)
	 *
	 * @return an array of aliases as strings.
	 */
	public String[] getReturnAliases() {
		return returnAliases;
	}

	/**
	 * An array of types describing the returns of the source query.
	 *
	 * @return The return type array.
	 */
	public Type[] getReturnTypes() {
		return returnTypes;
	}

	/**
	 * The set of query spaces affected by this source query.
	 *
	 * @return The set of query spaces.
	 */
	public Set getQuerySpaces() {
		return querySpaces;
	}
}
