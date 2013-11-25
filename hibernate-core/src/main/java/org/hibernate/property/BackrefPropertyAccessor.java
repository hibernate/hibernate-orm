/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
 */
package org.hibernate.property;
import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Represents a "back-reference" to the id of a collection owner.  A "back-reference" is pertinent in mapping scenarios
 * where we have a uni-directional one-to-many association in which only the many side is mapped.  In this case it is
 * the collection itself which is responsible for the FK value.
 * <p/>
 * In this scenario, the one side has no inherent knowledge of its "owner".  So we introduce a synthetic property into
 * the one side to represent the association; a so-called back-reference.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BackrefPropertyAccessor implements PropertyAccessor {

	private final String propertyName;
	private final String entityName;

	// cache these since they are stateless
	private final BackrefSetter setter; // this one could even be static...
	private final BackrefGetter getter;

	/**
	 * A placeholder for a property value, indicating that
	 * we don't know the value of the back reference
	 */
	public static final Serializable UNKNOWN = new Serializable() {
		@Override
		public String toString() {
			return "<unknown>";
		}

		public Object readResolve() {
			return UNKNOWN;
		}
	};

	/**
	 * Constructs a new instance of BackrefPropertyAccessor.
	 *
	 * @param collectionRole The collection role which this back ref references.
	 * @param entityName The owner's entity name.
	 */
	public BackrefPropertyAccessor(String collectionRole, String entityName) {
		this.propertyName = collectionRole.substring( entityName.length() + 1 );
		this.entityName = entityName;

		this.setter = new BackrefSetter();
		this.getter = new BackrefGetter();
	}

	@Override
	public Setter getSetter(Class theClass, String propertyName) {
		return setter;
	}

	@Override
	public Getter getGetter(Class theClass, String propertyName) {
		return getter;
	}


	/**
	 * Internal implementation of a property setter specific to these back-ref properties.
	 */
	public static final class BackrefSetter implements Setter {
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
			// this page intentionally left blank :)
		}

	}


	/**
	 * Internal implementation of a property getter specific to these back-ref properties.
	 */
	public class BackrefGetter implements Getter {
		@Override
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) {
			if ( session == null ) {
				return UNKNOWN;
			}
			else {
				return session.getPersistenceContext().getOwnerId( entityName, propertyName, target, mergeMap );
			}
		}

		@Override
		public Member getMember() {
			return null;
		}

		@Override
		public Object get(Object target) {
			return UNKNOWN;
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

