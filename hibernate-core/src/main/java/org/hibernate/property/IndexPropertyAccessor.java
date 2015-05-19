/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

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
	 * @param entityName The name of the entity owning the collection.
	 */
	public IndexPropertyAccessor(String collectionRole, String entityName) {
		this.propertyName = collectionRole.substring( entityName.length()+1 );
		this.entityName = entityName;
	}

	@Override
	public Setter getSetter(Class theClass, String propertyName) {
		return new IndexSetter();
	}

	@Override
	public Getter getGetter(Class theClass, String propertyName) {
		return new IndexGetter();
	}


	/**
	 * The Setter implementation for index backrefs.
	 */
	public static final class IndexSetter implements Setter {
		@Override
		public Method getMethod() {
			return null;
		}

		@Override
		public String getMethodName() {
			return null;
		}

		@Override
		public void set(Object target, Object value, SessionFactoryImplementor factory) {
			// do nothing...
		}
	}


	/**
	 * The Getter implementation for index backrefs.
	 */
	public class IndexGetter implements Getter {
		@Override
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) throws HibernateException {
			if ( session == null ) {
				return BackrefPropertyAccessor.UNKNOWN;
			}
			else {
				return session.getPersistenceContext().getIndexInOwner( entityName, propertyName, target, mergeMap );
			}
		}

		@Override
		public Object get(Object target)  {
			return BackrefPropertyAccessor.UNKNOWN;
		}

		@Override
		public Member getMember() {
			return null;
		}

		@Override
		public Method getMethod() {
			return null;
		}

		@Override
		public String getMethodName() {
			return null;
		}

		@Override
		public Class getReturnType() {
			return Object.class;
		}
	}
}
