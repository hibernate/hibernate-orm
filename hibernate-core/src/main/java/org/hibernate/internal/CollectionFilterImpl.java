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
package org.hibernate.internal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.Type;

/**
 * implementation of the <tt>Query</tt> interface for collection filters
 * @author Gavin King
 */
public class CollectionFilterImpl extends QueryImpl {

	private Object collection;

	public CollectionFilterImpl(
			String queryString,
	        Object collection,
	        SessionImplementor session,
	        ParameterMetadata parameterMetadata) {
		super( queryString, session, parameterMetadata );
		this.collection = collection;
	}


	/**
	 * @see org.hibernate.Query#iterate()
	 */
	public Iterator iterate() throws HibernateException {
		verifyParameters();
		Map namedParams = getNamedParams();
		return getSession().iterateFilter( 
				collection, 
				expandParameterLists(namedParams),
				getQueryParameters(namedParams) 
		);
	}

	/**
	 * @see org.hibernate.Query#list()
	 */
	public List list() throws HibernateException {
		verifyParameters();
		Map namedParams = getNamedParams();
		return getSession().listFilter( 
				collection, 
				expandParameterLists(namedParams),
				getQueryParameters(namedParams) 
		);
	}

	/**
	 * @see org.hibernate.Query#scroll()
	 */
	public ScrollableResults scroll() throws HibernateException {
		throw new UnsupportedOperationException("Can't scroll filters");
	}

	public Type[] typeArray() {
		List typeList = getTypes();
		int size = typeList.size();
		Type[] result = new Type[size+1];
		for (int i=0; i<size; i++) result[i+1] = (Type) typeList.get(i);
		return result;
	}

	public Object[] valueArray() {
		List valueList = getValues();
		int size = valueList.size();
		Object[] result = new Object[size+1];
		for (int i=0; i<size; i++) result[i+1] = valueList.get(i);
		return result;
	}

}
