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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.TypedValue;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * Support for query by example.
 * <pre>
 * List results = session.createCriteria(Parent.class)
 *     .add( Example.create(parent).ignoreCase() )
 *     .createCriteria("child")
 *         .add( Example.create( parent.getChild() ) )
 *     .list();
 * </pre>
 * "Examples" may be mixed and matched with "Expressions" in the same <tt>Criteria</tt>.
 * @see org.hibernate.Criteria
 * @author Gavin King
 */

public class Example implements Criterion {

	private final Object entity;
	private final Set excludedProperties = new HashSet();
	private PropertySelector selector;
	private boolean isLikeEnabled;
	private Character escapeCharacter;
	private boolean isIgnoreCaseEnabled;
	private MatchMode matchMode;

	/**
	 * A strategy for choosing property values for inclusion in the query
	 * criteria
	 */

	public static interface PropertySelector extends Serializable {
		public boolean include(Object propertyValue, String propertyName, Type type);
	}

	private static final PropertySelector NOT_NULL = new NotNullPropertySelector();
	private static final PropertySelector ALL = new AllPropertySelector();
	private static final PropertySelector NOT_NULL_OR_ZERO = new NotNullOrZeroPropertySelector();

	static final class AllPropertySelector implements PropertySelector {
		public boolean include(Object object, String propertyName, Type type) {
			return true;
		}
		
		private Object readResolve() {
			return ALL;
		}
	}

	static final class NotNullPropertySelector implements PropertySelector {
		public boolean include(Object object, String propertyName, Type type) {
			return object!=null;
		}
		
		private Object readResolve() {
			return NOT_NULL;
		}
	}

	static final class NotNullOrZeroPropertySelector implements PropertySelector {
		public boolean include(Object object, String propertyName, Type type) {
			return object!=null && (
				!(object instanceof Number) || ( (Number) object ).longValue()!=0
			);
		}
		
		private Object readResolve() {
			return NOT_NULL_OR_ZERO;
		}
	}

	/**
	 * Set escape character for "like" clause
	 */
	public Example setEscapeCharacter(Character escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
		return this;
	}

	/**
	 * Set the property selector
	 */
	public Example setPropertySelector(PropertySelector selector) {
		this.selector = selector;
		return this;
	}

	/**
	 * Exclude zero-valued properties
	 */
	public Example excludeZeroes() {
		setPropertySelector(NOT_NULL_OR_ZERO);
		return this;
	}

	/**
	 * Don't exclude null or zero-valued properties
	 */
	public Example excludeNone() {
		setPropertySelector(ALL);
		return this;
	}

	/**
	 * Use the "like" operator for all string-valued properties
	 */
	public Example enableLike(MatchMode matchMode) {
		isLikeEnabled = true;
		this.matchMode = matchMode;
		return this;
	}

	/**
	 * Use the "like" operator for all string-valued properties
	 */
	public Example enableLike() {
		return enableLike(MatchMode.EXACT);
	}

	/**
	 * Ignore case for all string-valued properties
	 */
	public Example ignoreCase() {
		isIgnoreCaseEnabled = true;
		return this;
	}

	/**
	 * Exclude a particular named property
	 */
	public Example excludeProperty(String name) {
		excludedProperties.add(name);
		return this;
	}

	/**
	 * Create a new instance, which includes all non-null properties
	 * by default
	 * @param entity
	 * @return a new instance of <tt>Example</tt>
	 */
	public static Example create(Object entity) {
		if (entity==null) throw new NullPointerException("null example");
		return new Example(entity, NOT_NULL);
	}

	protected Example(Object entity, PropertySelector selector) {
		this.entity = entity;
		this.selector = selector;
	}

	public String toString() {
		return "example (" + entity + ')';
	}

	private boolean isPropertyIncluded(Object value, String name, Type type) {
		return !excludedProperties.contains(name) &&
			!type.isAssociationType() &&
			selector.include(value, name, type);
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
		throws HibernateException {

		StringBuffer buf = new StringBuffer().append('(');
		EntityPersister meta = criteriaQuery.getFactory().getEntityPersister( criteriaQuery.getEntityName(criteria) );
		String[] propertyNames = meta.getPropertyNames();
		Type[] propertyTypes = meta.getPropertyTypes();
		//TODO: get all properties, not just the fetched ones!
		Object[] propertyValues = meta.getPropertyValues( entity, getEntityMode(criteria, criteriaQuery) );
		for (int i=0; i<propertyNames.length; i++) {
			Object propertyValue = propertyValues[i];
			String propertyName = propertyNames[i];

			boolean isPropertyIncluded = i!=meta.getVersionProperty() &&
				isPropertyIncluded( propertyValue, propertyName, propertyTypes[i] );
			if (isPropertyIncluded) {
				if ( propertyTypes[i].isComponentType() ) {
					appendComponentCondition(
						propertyName,
						propertyValue,
						(AbstractComponentType) propertyTypes[i],
						criteria,
						criteriaQuery,
						buf
					);
				}
				else {
					appendPropertyCondition(
						propertyName,
						propertyValue,
						criteria,
						criteriaQuery,
						buf
					);
				}
			}
		}
		if ( buf.length()==1 ) buf.append("1=1"); //yuck!
		return buf.append(')').toString();
	}

	private static final Object[] TYPED_VALUES = new TypedValue[0];

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {

		EntityPersister meta = criteriaQuery.getFactory()
				.getEntityPersister( criteriaQuery.getEntityName(criteria) );
		String[] propertyNames = meta.getPropertyNames();
		Type[] propertyTypes = meta.getPropertyTypes();
		 //TODO: get all properties, not just the fetched ones!
		Object[] values = meta.getPropertyValues( entity, getEntityMode(criteria, criteriaQuery) );
		List list = new ArrayList();
		for (int i=0; i<propertyNames.length; i++) {
			Object value = values[i];
			Type type = propertyTypes[i];
			String name = propertyNames[i];

			boolean isPropertyIncluded = i!=meta.getVersionProperty() &&
				isPropertyIncluded(value, name, type);

			if (isPropertyIncluded) {
				if ( propertyTypes[i].isComponentType() ) {
					addComponentTypedValues(name, value, (AbstractComponentType) type, list, criteria, criteriaQuery);
				}
				else {
					addPropertyTypedValue(value, type, list);
				}
			}
		}
		return (TypedValue[]) list.toArray(TYPED_VALUES);
	}
	
	private EntityMode getEntityMode(Criteria criteria, CriteriaQuery criteriaQuery) {
		EntityPersister meta = criteriaQuery.getFactory()
				.getEntityPersister( criteriaQuery.getEntityName(criteria) );
		EntityMode result = meta.guessEntityMode(entity);
		if (result==null) {
			throw new ClassCastException( entity.getClass().getName() );
		}
		return result;
	}

	protected void addPropertyTypedValue(Object value, Type type, List list) {
		if ( value!=null ) {
			if ( value instanceof String ) {
				String string = (String) value;
				if (isIgnoreCaseEnabled) string = string.toLowerCase();
				if (isLikeEnabled) string = matchMode.toMatchString(string);
				value = string;
			}
			list.add( new TypedValue(type, value, null) );
		}
	}

	protected void addComponentTypedValues(
			String path, 
			Object component, 
			AbstractComponentType type, 
			List list, 
			Criteria criteria, 
			CriteriaQuery criteriaQuery)
	throws HibernateException {

		if (component!=null) {
			String[] propertyNames = type.getPropertyNames();
			Type[] subtypes = type.getSubtypes();
			Object[] values = type.getPropertyValues( component, getEntityMode(criteria, criteriaQuery) );
			for (int i=0; i<propertyNames.length; i++) {
				Object value = values[i];
				Type subtype = subtypes[i];
				String subpath = StringHelper.qualify( path, propertyNames[i] );
				if ( isPropertyIncluded(value, subpath, subtype) ) {
					if ( subtype.isComponentType() ) {
						addComponentTypedValues(subpath, value, (AbstractComponentType) subtype, list, criteria, criteriaQuery);
					}
					else {
						addPropertyTypedValue(value, subtype, list);
					}
				}
			}
		}
	}

	protected void appendPropertyCondition(
		String propertyName,
		Object propertyValue,
		Criteria criteria,
		CriteriaQuery cq,
		StringBuffer buf)
	throws HibernateException {
		Criterion crit;
		if ( propertyValue!=null ) {
			boolean isString = propertyValue instanceof String;
			if ( isLikeEnabled && isString ) {
				crit = new LikeExpression(
						propertyName,
						( String ) propertyValue,
						matchMode,
						escapeCharacter,
						isIgnoreCaseEnabled
				);
			}
			else {
				crit = new SimpleExpression( propertyName, propertyValue, "=", isIgnoreCaseEnabled && isString );
			}
		}
		else {
			crit = new NullExpression(propertyName);
		}
		String critCondition = crit.toSqlString(criteria, cq);
		if ( buf.length()>1 && critCondition.trim().length()>0 ) buf.append(" and ");
		buf.append(critCondition);
	}

	protected void appendComponentCondition(
		String path,
		Object component,
		AbstractComponentType type,
		Criteria criteria,
		CriteriaQuery criteriaQuery,
		StringBuffer buf)
	throws HibernateException {

		if (component!=null) {
			String[] propertyNames = type.getPropertyNames();
			Object[] values = type.getPropertyValues( component, getEntityMode(criteria, criteriaQuery) );
			Type[] subtypes = type.getSubtypes();
			for (int i=0; i<propertyNames.length; i++) {
				String subpath = StringHelper.qualify( path, propertyNames[i] );
				Object value = values[i];
				if ( isPropertyIncluded( value, subpath, subtypes[i] ) ) {
					Type subtype = subtypes[i];
					if ( subtype.isComponentType() ) {
						appendComponentCondition(
							subpath,
							value,
							(AbstractComponentType) subtype,
							criteria,
							criteriaQuery,
							buf
						);
					}
					else {
						appendPropertyCondition(
							subpath,
							value,
							criteria,
							criteriaQuery,
							buf
						);
					}
				}
			}
		}
	}
}