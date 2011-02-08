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
package org.hibernate.transform;
import java.io.Serializable;
import java.util.List;

/**
 * Implementors define a strategy for transforming query results into the
 * actual application-visible query result list.
 *
 * @see org.hibernate.Criteria#setResultTransformer(ResultTransformer)
 * @see org.hibernate.Query#setResultTransformer(ResultTransformer)
 *
 * @author Gavin King
 */
public interface ResultTransformer extends Serializable {
	/**
	 * Tuples are the elements making up each "row" of the query result.
	 * The contract here is to transform these elements into the final
	 * row.
	 *
	 * @param tuple The result elements
	 * @param aliases The result aliases ("parallel" array to tuple)
	 * @return The transformed row.
	 */
	public Object transformTuple(Object[] tuple, String[] aliases);

	/**
	 * Here we have an opportunity to perform transformation on the
	 * query result as a whole.  This might be useful to convert from
	 * one collection type to another or to remove duplicates from the
	 * result, etc.
	 *
	 * @param collection The result.
	 * @return The transformed result.
	 */
	public List transformList(List collection);
}
