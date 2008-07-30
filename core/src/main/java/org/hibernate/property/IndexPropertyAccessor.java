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
package org.hibernate.property;

import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Represents a "back-reference" to the index of a collection.
 *
 * @author Gavin King
 */
public class IndexPropertyAccessor implements PropertyAccessor {
	
	private final String propertyName;
	private final String entityName;

	/**
	 * Constructs a new instance of IndexPropertyAccessor.
	 *
	 * @param collectionRole The collection role which this back ref references.
	 */
	public IndexPropertyAccessor(String collectionRole, String entityName) {
		this.propertyName = collectionRole.substring( entityName.length()+1 );
		this.entityName = entityName;
	}

	public Setter getSetter(Class theClass, String propertyName) {
		return new IndexSetter();
	}

	public Getter getGetter(Class theClass, String propertyName) {
		return new IndexGetter();
	}


	/**
	 * The Setter implementation for index backrefs.
	 */
	public static final class IndexSetter implements Setter {

		public Method getMethod() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public void set(Object target, Object value) {
			// do nothing...
		}

		public void set(Object target, Object value, SessionFactoryImplementor factory) throws HibernateException {
			// do nothing...
		}

	}


	/**
	 * The Getter implementation for index backrefs.
	 */
	public class IndexGetter implements Getter {
		
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) throws HibernateException {
			if (session==null) {
				return BackrefPropertyAccessor.UNKNOWN;
			}
			else {
				return session.getPersistenceContext()
						.getIndexInOwner(entityName, propertyName, target, mergeMap);
			}
		}

		public Object get(Object target)  {
			return BackrefPropertyAccessor.UNKNOWN;
		}

		public Method getMethod() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public Class getReturnType() {
			return Object.class;
		}
	}
}
