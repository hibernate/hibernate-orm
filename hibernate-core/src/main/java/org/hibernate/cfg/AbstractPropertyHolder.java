/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.cfg;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MappedSuperclass;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public abstract class AbstractPropertyHolder implements PropertyHolder {
	protected AbstractPropertyHolder parent;
	private Map<String, Column[]> holderColumnOverride;
	private Map<String, Column[]> currentPropertyColumnOverride;
	private Map<String, JoinColumn[]> holderJoinColumnOverride;
	private Map<String, JoinColumn[]> currentPropertyJoinColumnOverride;
	private Map<String, JoinTable> holderJoinTableOverride;
	private Map<String, JoinTable> currentPropertyJoinTableOverride;
	private String path;
	private Mappings mappings;
	private Boolean isInIdClass;


	public AbstractPropertyHolder(
			String path,
			PropertyHolder parent,
			XClass clazzToProcess,
			Mappings mappings) {
		this.path = path;
		this.parent = (AbstractPropertyHolder) parent;
		this.mappings = mappings;
		buildHierarchyColumnOverride( clazzToProcess );
	}


	public boolean isInIdClass() {
		return isInIdClass != null ? isInIdClass : parent != null ? parent.isInIdClass() : false;
	}

	public void setInIdClass(Boolean isInIdClass) {
		this.isInIdClass = isInIdClass;
	}

	public String getPath() {
		return path;
	}

	protected Mappings getMappings() {
		return mappings;
	}

	/**
	 * property can be null
	 */
	protected void setCurrentProperty(XProperty property) {
		if ( property == null ) {
			this.currentPropertyColumnOverride = null;
			this.currentPropertyJoinColumnOverride = null;
			this.currentPropertyJoinTableOverride = null;
		}
		else {
			this.currentPropertyColumnOverride = buildColumnOverride(
					property,
					getPath()
			);
			if ( this.currentPropertyColumnOverride.size() == 0 ) {
				this.currentPropertyColumnOverride = null;
			}
			this.currentPropertyJoinColumnOverride = buildJoinColumnOverride(
					property,
					getPath()
			);
			if ( this.currentPropertyJoinColumnOverride.size() == 0 ) {
				this.currentPropertyJoinColumnOverride = null;
			}
			this.currentPropertyJoinTableOverride = buildJoinTableOverride(
					property,
					getPath()
			);
			if ( this.currentPropertyJoinTableOverride.size() == 0 ) {
				this.currentPropertyJoinTableOverride = null;
			}
		}
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&&element' with nothing
	 *
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 *
	 */
	public Column[] getOverriddenColumn(String propertyName) {
		Column[] result = getExactOverriddenColumn( propertyName );
		if (result == null) {
			//the commented code can be useful if people use the new prefixes on old mappings and vice versa
			// if we enable them:
			// WARNING: this can conflict with user's expectations if:
	 		//  - the property uses some restricted values
	 		//  - the user has overridden the column
			// also change getOverriddenJoinColumn and getOverriddenJoinTable as well
	 		
//			if ( propertyName.contains( ".key." ) ) {
//				//support for legacy @AttributeOverride declarations
//				//TODO cache the underlying regexp
//				result = getExactOverriddenColumn( propertyName.replace( ".key.", ".index."  ) );
//			}
//			if ( result == null && propertyName.endsWith( ".key" ) ) {
//				//support for legacy @AttributeOverride declarations
//				//TODO cache the underlying regexp
//				result = getExactOverriddenColumn(
//						propertyName.substring( 0, propertyName.length() - ".key".length() ) + ".index"
//						);
//			}
//			if ( result == null && propertyName.contains( ".value." ) ) {
//				//support for legacy @AttributeOverride declarations
//				//TODO cache the underlying regexp
//				result = getExactOverriddenColumn( propertyName.replace( ".value.", ".element."  ) );
//			}
//			if ( result == null && propertyName.endsWith( ".value" ) ) {
//				//support for legacy @AttributeOverride declarations
//				//TODO cache the underlying regexp
//				result = getExactOverriddenColumn(
//						propertyName.substring( 0, propertyName.length() - ".value".length() ) + ".element"
//						);
//			}
			if ( result == null && propertyName.contains( ".collection&&element." ) ) {
				//support for non map collections where no prefix is needed
				//TODO cache the underlying regexp
				result = getExactOverriddenColumn( propertyName.replace( ".collection&&element.", "."  ) );
			}
		}
		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * find the overridden rules from the exact property name.
	 */
	private Column[] getExactOverriddenColumn(String propertyName) {
		Column[] override = null;
		if ( parent != null ) {
			override = parent.getExactOverriddenColumn( propertyName );
		}
		if ( override == null && currentPropertyColumnOverride != null ) {
			override = currentPropertyColumnOverride.get( propertyName );
		}
		if ( override == null && holderColumnOverride != null ) {
			override = holderColumnOverride.get( propertyName );
		}
		return override;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&&element' with nothing
	 *
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 *
	 */
	public JoinColumn[] getOverriddenJoinColumn(String propertyName) {
		JoinColumn[] result = getExactOverriddenJoinColumn( propertyName );
		if ( result == null && propertyName.contains( ".collection&&element." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			result = getExactOverriddenJoinColumn( propertyName.replace( ".collection&&element.", "."  ) );
		}
		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 */
	private JoinColumn[] getExactOverriddenJoinColumn(String propertyName) {
		JoinColumn[] override = null;
		if ( parent != null ) {
			override = parent.getExactOverriddenJoinColumn( propertyName );
		}
		if ( override == null && currentPropertyJoinColumnOverride != null ) {
			override = currentPropertyJoinColumnOverride.get( propertyName );
		}
		if ( override == null && holderJoinColumnOverride != null ) {
			override = holderJoinColumnOverride.get( propertyName );
		}
		return override;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&&element' with nothing
	 *
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 *
	 */
	public JoinTable getJoinTable(XProperty property) {
		final String propertyName = StringHelper.qualify( getPath(), property.getName() );
		JoinTable result = getOverriddenJoinTable( propertyName );
		if (result == null) {
			result = property.getAnnotation( JoinTable.class );
		}
		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 * replace the placeholder 'collection&&element' with nothing
	 *
	 * These rules are here to support both JPA 2 and legacy overriding rules.
	 *
	 */
	public JoinTable getOverriddenJoinTable(String propertyName) {
		JoinTable result = getExactOverriddenJoinTable( propertyName );
		if ( result == null && propertyName.contains( ".collection&&element." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			result = getExactOverriddenJoinTable( propertyName.replace( ".collection&&element.", "."  ) );
		}
		return result;
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 */
	private JoinTable getExactOverriddenJoinTable(String propertyName) {
		JoinTable override = null;
		if ( parent != null ) {
			override = parent.getExactOverriddenJoinTable( propertyName );
		}
		if ( override == null && currentPropertyJoinTableOverride != null ) {
			override = currentPropertyJoinTableOverride.get( propertyName );
		}
		if ( override == null && holderJoinTableOverride != null ) {
			override = holderJoinTableOverride.get( propertyName );
		}
		return override;
	}

	private void buildHierarchyColumnOverride(XClass element) {
		XClass current = element;
		Map<String, Column[]> columnOverride = new HashMap<String, Column[]>();
		Map<String, JoinColumn[]> joinColumnOverride = new HashMap<String, JoinColumn[]>();
		Map<String, JoinTable> joinTableOverride = new HashMap<String, JoinTable>();
		while ( current != null && !mappings.getReflectionManager().toXClass( Object.class ).equals( current ) ) {
			if ( current.isAnnotationPresent( Entity.class ) || current.isAnnotationPresent( MappedSuperclass.class )
					|| current.isAnnotationPresent( Embeddable.class ) ) {
				//FIXME is embeddable override?
				Map<String, Column[]> currentOverride = buildColumnOverride( current, getPath() );
				Map<String, JoinColumn[]> currentJoinOverride = buildJoinColumnOverride( current, getPath() );
				Map<String, JoinTable> currentJoinTableOverride = buildJoinTableOverride( current, getPath() );
				currentOverride.putAll( columnOverride ); //subclasses have precedence over superclasses
				currentJoinOverride.putAll( joinColumnOverride ); //subclasses have precedence over superclasses
				currentJoinTableOverride.putAll( joinTableOverride ); //subclasses have precedence over superclasses
				columnOverride = currentOverride;
				joinColumnOverride = currentJoinOverride;
				joinTableOverride = currentJoinTableOverride;
			}
			current = current.getSuperclass();
		}

		holderColumnOverride = columnOverride.size() > 0 ? columnOverride : null;
		holderJoinColumnOverride = joinColumnOverride.size() > 0 ? joinColumnOverride : null;
		holderJoinTableOverride = joinTableOverride.size() > 0 ? joinTableOverride : null;
	}

	private static Map<String, Column[]> buildColumnOverride(XAnnotatedElement element, String path) {
		Map<String, Column[]> columnOverride = new HashMap<String, Column[]>();
		if ( element == null ) return columnOverride;
		AttributeOverride singleOverride = element.getAnnotation( AttributeOverride.class );
		AttributeOverrides multipleOverrides = element.getAnnotation( AttributeOverrides.class );
		AttributeOverride[] overrides;
		if ( singleOverride != null ) {
			overrides = new AttributeOverride[] { singleOverride };
		}
		else if ( multipleOverrides != null ) {
			overrides = multipleOverrides.value();
		}
		else {
			overrides = null;
		}

		//fill overridden columns
		if ( overrides != null ) {
			for (AttributeOverride depAttr : overrides) {
				columnOverride.put(
						StringHelper.qualify( path, depAttr.name() ),
						new Column[] { depAttr.column() }
				);
			}
		}
		return columnOverride;
	}

	private static Map<String, JoinColumn[]> buildJoinColumnOverride(XAnnotatedElement element, String path) {
		Map<String, JoinColumn[]> columnOverride = new HashMap<String, JoinColumn[]>();
		if ( element == null ) return columnOverride;
		AssociationOverride singleOverride = element.getAnnotation( AssociationOverride.class );
		AssociationOverrides multipleOverrides = element.getAnnotation( AssociationOverrides.class );
		AssociationOverride[] overrides;
		if ( singleOverride != null ) {
			overrides = new AssociationOverride[] { singleOverride };
		}
		else if ( multipleOverrides != null ) {
			overrides = multipleOverrides.value();
		}
		else {
			overrides = null;
		}

		//fill overridden columns
		if ( overrides != null ) {
			for (AssociationOverride depAttr : overrides) {
				columnOverride.put(
						StringHelper.qualify( path, depAttr.name() ),
						depAttr.joinColumns()
				);
			}
		}
		return columnOverride;
	}

	private static Map<String, JoinTable> buildJoinTableOverride(XAnnotatedElement element, String path) {
		Map<String, JoinTable> tableOverride = new HashMap<String, JoinTable>();
		if ( element == null ) return tableOverride;
		AssociationOverride singleOverride = element.getAnnotation( AssociationOverride.class );
		AssociationOverrides multipleOverrides = element.getAnnotation( AssociationOverrides.class );
		AssociationOverride[] overrides;
		if ( singleOverride != null ) {
			overrides = new AssociationOverride[] { singleOverride };
		}
		else if ( multipleOverrides != null ) {
			overrides = multipleOverrides.value();
		}
		else {
			overrides = null;
		}

		//fill overridden tables
		if ( overrides != null ) {
			for (AssociationOverride depAttr : overrides) {
				if ( depAttr.joinColumns().length == 0 ) {
					tableOverride.put(
							StringHelper.qualify( path, depAttr.name() ),
							depAttr.joinTable()
					);
				}
			}
		}
		return tableOverride;
	}

	public void setParentProperty(String parentProperty) {
		throw new AssertionFailure( "Setting the parent property to a non component" );
	}
}
