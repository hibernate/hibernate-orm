//$Id$
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
import javax.persistence.MappedSuperclass;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public abstract class AbstractPropertyHolder implements PropertyHolder {
	protected PropertyHolder parent;
	private Map<String, Column[]> holderColumnOverride;
	private Map<String, Column[]> currentPropertyColumnOverride;
	private Map<String, JoinColumn[]> holderJoinColumnOverride;
	private Map<String, JoinColumn[]> currentPropertyJoinColumnOverride;
	private String path;
	private ExtendedMappings mappings;

	public AbstractPropertyHolder(
			String path, PropertyHolder parent, XClass clazzToProcess, ExtendedMappings mappings
	) {
		this.path = path;
		this.parent = parent;
		this.mappings = mappings;
		buildHierarchyColumnOverride( clazzToProcess );
	}

	public String getPath() {
		return path;
	}

	/**
	 * property can be null
	 */
	protected void setCurrentProperty(XProperty property) {
		if ( property == null ) {
			this.currentPropertyColumnOverride = null;
			this.currentPropertyJoinColumnOverride = null;
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
		}
	}

	/**
	 * Get column overriding, property first, then parent, then holder
	 */
	public Column[] getOverriddenColumn(String propertyName) {
		Column[] override = null;
		if ( parent != null ) {
			override = parent.getOverriddenColumn( propertyName );
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
	 */
	public JoinColumn[] getOverriddenJoinColumn(String propertyName) {
		JoinColumn[] override = null;
		if ( parent != null ) {
			override = parent.getOverriddenJoinColumn( propertyName );
		}
		if ( override == null && currentPropertyJoinColumnOverride != null ) {
			override = currentPropertyJoinColumnOverride.get( propertyName );
		}
		if ( override == null && holderJoinColumnOverride != null ) {
			override = holderJoinColumnOverride.get( propertyName );
		}
		return override;
	}

	private void buildHierarchyColumnOverride(XClass element) {
		XClass current = element;
		Map<String, Column[]> columnOverride = new HashMap<String, Column[]>();
		Map<String, JoinColumn[]> joinColumnOverride = new HashMap<String, JoinColumn[]>();
		while ( current != null && !mappings.getReflectionManager().toXClass( Object.class ).equals( current ) ) {
			if ( current.isAnnotationPresent( Entity.class ) || current.isAnnotationPresent( MappedSuperclass.class )
					|| current.isAnnotationPresent( Embeddable.class ) ) {
				//FIXME is embeddable override?
				Map<String, Column[]> currentOverride = buildColumnOverride( current, getPath() );
				Map<String, JoinColumn[]> currentJoinOverride = buildJoinColumnOverride( current, getPath() );
				currentOverride.putAll( columnOverride ); //subclasses have precedence over superclasses
				currentJoinOverride.putAll( joinColumnOverride ); //subclasses have precedence over superclasses
				columnOverride = currentOverride;
				joinColumnOverride = currentJoinOverride;
			}
			current = current.getSuperclass();
		}

		holderColumnOverride = columnOverride.size() > 0 ? columnOverride : null;
		holderJoinColumnOverride = joinColumnOverride.size() > 0 ? joinColumnOverride : null;
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

		//fill overriden columns
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

		//fill overriden columns
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

	public void setParentProperty(String parentProperty) {
		throw new AssertionFailure( "Setting the parent property to a non component" );
	}
}
