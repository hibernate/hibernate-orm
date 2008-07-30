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

import java.lang.reflect.Constructor;
import java.util.List;

import org.hibernate.QueryException;

public class AliasToBeanConstructorResultTransformer implements ResultTransformer {

	private Constructor constructor;

	public AliasToBeanConstructorResultTransformer(Constructor constructor) {
		this.constructor = constructor;
	}
	
	public Object transformTuple(Object[] tuple, String[] aliases) {
		try {
			return constructor.newInstance( tuple );
		}
		catch ( Exception e ) {
			throw new QueryException( 
					"could not instantiate: " + 
					constructor.getDeclaringClass().getName(), 
					e );
		}
	}

	public List transformList(List collection) {
		return collection;
	}

	
}
