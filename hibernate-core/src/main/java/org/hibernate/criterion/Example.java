/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * Support for query by example.
 *
 * <pre>
 * List results = session.createCriteria(Parent.class)
 *     .add( Example.create(parent).ignoreCase() )
 *     .createCriteria("child")
 *         .add( Example.create( parent.getChild() ) )
 *     .list();
 * </pre>
 *
 * "Examples" may be mixed and matched with "Expressions" in the same Criteria.
 *
 * @see org.hibernate.Criteria
 * @author Gavin King
 */

public class Example implements Criterion {
	private final Object exampleEntity;
	private PropertySelector selector;

	private boolean isLikeEnabled;
	private Character escapeCharacter;
	private boolean isIgnoreCaseEnabled;
	private MatchMode matchMode;

	private final Set<String> excludedProperties = new HashSet<String>();

	/**
	 * Create a new Example criterion instance, which includes all non-null properties by default
	 *
	 * @param exampleEntity The example bean to use.
	 *
	 * @return a new instance of Example
	 */
	public static Example create(Object exampleEntity) {
		if ( exampleEntity == null ) {
			throw new NullPointerException( "null example entity" );
		}
		return new Example( exampleEntity, NotNullPropertySelector.INSTANCE );
	}

	/**
	 * Allow subclasses to instantiate as needed.
	 *
	 * @param exampleEntity The example bean
	 * @param selector The property selector to use
	 */
	protected Example(Object exampleEntity, PropertySelector selector) {
		this.exampleEntity = exampleEntity;
		this.selector = selector;
	}

	/**
	 * Set escape character for "like" clause if like matching was enabled
	 *
	 * @param escapeCharacter The escape character
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #enableLike
	 */
	public Example setEscapeCharacter(Character escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
		return this;
	}

	/**
	 * Use the "like" operator for all string-valued properties.  This form implicitly uses {@link MatchMode#EXACT}
	 *
	 * @return {@code this}, for method chaining
	 */
	public Example enableLike() {
		return enableLike( MatchMode.EXACT );
	}

	/**
	 * Use the "like" operator for all string-valued properties
	 *
	 * @param matchMode The match mode to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public Example enableLike(MatchMode matchMode) {
		this.isLikeEnabled = true;
		this.matchMode = matchMode;
		return this;
	}

	/**
	 * Ignore case for all string-valued properties
	 *
	 * @return {@code this}, for method chaining
	 */
	public Example ignoreCase() {
		this.isIgnoreCaseEnabled = true;
		return this;
	}

	/**
	 * Set the property selector to use.
	 *
	 * The property selector operates separate from excluding a property.
	 *
	 * @param selector The selector to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #excludeProperty
	 */
	public Example setPropertySelector(PropertySelector selector) {
		this.selector = selector;
		return this;
	}

	/**
	 * Exclude zero-valued properties.
	 *
	 * Equivalent to calling {@link #setPropertySelector} passing in {@link NotNullOrZeroPropertySelector#INSTANCE}
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #setPropertySelector
	 */
	public Example excludeZeroes() {
		setPropertySelector( NotNullOrZeroPropertySelector.INSTANCE );
		return this;
	}

	/**
	 * Include all properties.
	 *
	 * Equivalent to calling {@link #setPropertySelector} passing in {@link AllPropertySelector#INSTANCE}
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #setPropertySelector
	 */
	public Example excludeNone() {
		setPropertySelector( AllPropertySelector.INSTANCE );
		return this;
	}

	/**
	 * Exclude a particular property by name.
	 *
	 * @param name The name of the property to exclude
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #setPropertySelector
	 */
	public Example excludeProperty(String name) {
		excludedProperties.add( name );
		return this;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		final StringBuilder buf = new StringBuilder().append( '(' );
		final EntityPersister meta = criteriaQuery.getFactory().getEntityPersister(
				criteriaQuery.getEntityName( criteria )
		);
		final String[] propertyNames = meta.getPropertyNames();
		final Type[] propertyTypes = meta.getPropertyTypes();

		final Object[] propertyValues = meta.getPropertyValues( exampleEntity );
		for ( int i=0; i<propertyNames.length; i++ ) {
			final Object propertyValue = propertyValues[i];
			final String propertyName = propertyNames[i];

			final boolean isVersionProperty = i == meta.getVersionProperty();
			if ( ! isVersionProperty && isPropertyIncluded( propertyValue, propertyName, propertyTypes[i] ) ) {
				if ( propertyTypes[i].isComponentType() ) {
					appendComponentCondition(
						propertyName,
						propertyValue,
						(CompositeType) propertyTypes[i],
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

		if ( buf.length()==1 ) {
			buf.append( "1=1" );
		}

		return buf.append( ')' ).toString();
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean isPropertyIncluded(Object value, String name, Type type) {
		if ( excludedProperties.contains( name ) ) {
			// was explicitly excluded
			return false;
		}

		if ( type.isAssociationType() ) {
			// associations are implicitly excluded
			return false;
		}

		return selector.include( value, name, type );
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		final EntityPersister meta = criteriaQuery.getFactory().getEntityPersister(
				criteriaQuery.getEntityName( criteria )
		);
		final String[] propertyNames = meta.getPropertyNames();
		final Type[] propertyTypes = meta.getPropertyTypes();

		final Object[] values = meta.getPropertyValues( exampleEntity );
		final List<TypedValue> list = new ArrayList<TypedValue>();
		for ( int i=0; i<propertyNames.length; i++ ) {
			final Object value = values[i];
			final Type type = propertyTypes[i];
			final String name = propertyNames[i];

			final boolean isVersionProperty = i == meta.getVersionProperty();

			if ( ! isVersionProperty && isPropertyIncluded( value, name, type ) ) {
				if ( propertyTypes[i].isComponentType() ) {
					addComponentTypedValues( name, value, (CompositeType) type, list, criteria, criteriaQuery );
				}
				else {
					addPropertyTypedValue( value, type, list );
				}
			}
		}

		return list.toArray( new TypedValue[ list.size() ] );
	}

	protected void addPropertyTypedValue(Object value, Type type, List<TypedValue> list) {
		if ( value != null ) {
			if ( value instanceof String ) {
				String string = (String) value;
				if ( isIgnoreCaseEnabled ) {
					string = string.toLowerCase(Locale.ROOT);
				}
				if ( isLikeEnabled ) {
					string = matchMode.toMatchString( string );
				}
				value = string;
			}
			list.add( new TypedValue( type, value ) );
		}
	}

	protected void addComponentTypedValues(
			String path, 
			Object component, 
			CompositeType type,
			List<TypedValue> list,
			Criteria criteria, 
			CriteriaQuery criteriaQuery) {
		if ( component != null ) {
			final String[] propertyNames = type.getPropertyNames();
			final Type[] subtypes = type.getSubtypes();
			final Object[] values = type.getPropertyValues( component, getEntityMode( criteria, criteriaQuery ) );
			for ( int i=0; i<propertyNames.length; i++ ) {
				final Object value = values[i];
				final Type subtype = subtypes[i];
				final String subpath = StringHelper.qualify( path, propertyNames[i] );
				if ( isPropertyIncluded( value, subpath, subtype ) ) {
					if ( subtype.isComponentType() ) {
						addComponentTypedValues( subpath, value, (CompositeType) subtype, list, criteria, criteriaQuery );
					}
					else {
						addPropertyTypedValue( value, subtype, list );
					}
				}
			}
		}
	}

	private EntityMode getEntityMode(Criteria criteria, CriteriaQuery criteriaQuery) {
		final EntityPersister meta = criteriaQuery.getFactory().getEntityPersister(
				criteriaQuery.getEntityName( criteria )
		);
		final EntityMode result = meta.getEntityMode();
		if ( ! meta.getEntityMetamodel().getTuplizer().isInstance( exampleEntity ) ) {
			throw new ClassCastException( exampleEntity.getClass().getName() );
		}
		return result;
	}

	protected void appendPropertyCondition(
			String propertyName,
			Object propertyValue,
			Criteria criteria,
			CriteriaQuery cq,
			StringBuilder buf) {
		final Criterion condition;
		if ( propertyValue != null ) {
			final boolean isString = propertyValue instanceof String;
			if ( isLikeEnabled && isString ) {
				condition = new LikeExpression(
						propertyName,
						(String) propertyValue,
						matchMode,
						escapeCharacter,
						isIgnoreCaseEnabled
				);
			}
			else {
				condition = new SimpleExpression( propertyName, propertyValue, "=", isIgnoreCaseEnabled && isString );
			}
		}
		else {
			condition = new NullExpression(propertyName);
		}

		final String conditionFragment = condition.toSqlString( criteria, cq );
		if ( conditionFragment.trim().length() > 0 ) {
			if ( buf.length() > 1 ) {
				buf.append( " and " );
			}
			buf.append( conditionFragment );
		}
	}

	protected void appendComponentCondition(
			String path,
			Object component,
			CompositeType type,
			Criteria criteria,
			CriteriaQuery criteriaQuery,
			StringBuilder buf) {
		if ( component != null ) {
			final String[] propertyNames = type.getPropertyNames();
			final Object[] values = type.getPropertyValues( component, getEntityMode( criteria, criteriaQuery ) );
			final Type[] subtypes = type.getSubtypes();
			for ( int i=0; i<propertyNames.length; i++ ) {
				final String subPath = StringHelper.qualify( path, propertyNames[i] );
				final Object value = values[i];
				if ( isPropertyIncluded( value, subPath, subtypes[i] ) ) {
					final Type subtype = subtypes[i];
					if ( subtype.isComponentType() ) {
						appendComponentCondition(
								subPath,
								value,
								(CompositeType) subtype,
								criteria,
								criteriaQuery,
								buf
						);
					}
					else {
						appendPropertyCondition(
								subPath,
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

	@Override
	public String toString() {
		return "example (" + exampleEntity + ')';
	}


	// PropertySelector definitions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * A strategy for choosing property values for inclusion in the query criteria.  Note that
	 * property selection (for inclusion) operates separately from excluding a property.  Excluded
	 * properties are not even passed in to the PropertySelector for consideration.
	 */
	public static interface PropertySelector extends Serializable {
		/**
		 * Determine whether the given property should be used in the criteria.
		 *
		 * @param propertyValue The property value (from the example bean)
		 * @param propertyName The name of the property
		 * @param type The type of the property
		 *
		 * @return {@code true} indicates the property should be included; {@code false} indiates it should not.
		 */
		public boolean include(Object propertyValue, String propertyName, Type type);
	}

	/**
	 * Property selector that includes all properties
	 */
	public static final class AllPropertySelector implements PropertySelector {
		/**
		 * Singleton access
		 */
		public static final AllPropertySelector INSTANCE = new AllPropertySelector();

		@Override
		public boolean include(Object object, String propertyName, Type type) {
			return true;
		}

		private Object readResolve() {
			return INSTANCE;
		}
	}

	/**
	 * Property selector that includes only properties that are not {@code null}
	 */
	public static final class NotNullPropertySelector implements PropertySelector {
		/**
		 * Singleton access
		 */
		public static final NotNullPropertySelector INSTANCE = new NotNullPropertySelector();

		@Override
		public boolean include(Object object, String propertyName, Type type) {
			return object!=null;
		}

		private Object readResolve() {
			return INSTANCE;
		}
	}

	/**
	 * Property selector that includes only properties that are not {@code null} and non-zero (if numeric)
	 */
	public static final class NotNullOrZeroPropertySelector implements PropertySelector {
		/**
		 * Singleton access
		 */
		public static final NotNullOrZeroPropertySelector INSTANCE = new NotNullOrZeroPropertySelector();

		@Override
		public boolean include(Object object, String propertyName, Type type) {
			return object != null
					&& ( !(object instanceof Number) || ( (Number) object ).longValue()!=0
			);
		}

		private Object readResolve() {
			return INSTANCE;
		}
	}
}
