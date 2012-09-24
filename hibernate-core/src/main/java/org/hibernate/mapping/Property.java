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
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * Represents a property as part of an entity or a component.
 *
 * @author Gavin King
 */
public class Property implements Serializable, MetaAttributable {
	private String name;
	private Value value;
	private String cascade;
	private boolean updateable = true;
	private boolean insertable = true;
	private boolean selectable = true;
	private boolean optimisticLocked = true;
	private PropertyGeneration generation = PropertyGeneration.NEVER;
	private String propertyAccessorName;
	private boolean lazy;
	private boolean optional;
	private String nodeName;
	private java.util.Map metaAttributes;
	private PersistentClass persistentClass;
	private boolean naturalIdentifier;
	private boolean lob;

	public boolean isBackRef() {
		return false;
	}

	/**
	 * Does this property represent a synthetic property?  A synthetic property is one we create during
	 * metamodel binding to represent a collection of columns but which does not represent a property
	 * physically available on the entity.
	 *
	 * @return True if synthetic; false otherwise.
	 */
	public boolean isSynthetic() {
		return false;
	}

	public Type getType() throws MappingException {
		return value.getType();
	}
	
	public int getColumnSpan() {
		return value.getColumnSpan();
	}
	
	public Iterator getColumnIterator() {
		return value.getColumnIterator();
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isComposite() {
		return value instanceof Component;
	}

	public Value getValue() {
		return value;
	}
	
	public boolean isPrimitive(Class clazz) {
		return getGetter(clazz).getReturnType().isPrimitive();
	}

	public CascadeStyle getCascadeStyle() throws MappingException {
		Type type = value.getType();
		if ( type.isComponentType() ) {
			return getCompositeCascadeStyle( (CompositeType) type, cascade );
		}
		else if ( type.isCollectionType() ) {
			return getCollectionCascadeStyle( ( (Collection) value ).getElement().getType(), cascade );
		}
		else {
			return getCascadeStyle( cascade );			
		}
	}

	private static CascadeStyle getCompositeCascadeStyle(CompositeType compositeType, String cascade) {
		if ( compositeType.isAnyType() ) {
			return getCascadeStyle( cascade );
		}
		int length = compositeType.getSubtypes().length;
		for ( int i=0; i<length; i++ ) {
			if ( compositeType.getCascadeStyle(i) != CascadeStyle.NONE ) {
				return CascadeStyle.ALL;
			}
		}
		return getCascadeStyle( cascade );
	}

	private static CascadeStyle getCollectionCascadeStyle(Type elementType, String cascade) {
		if ( elementType.isComponentType() ) {
			return getCompositeCascadeStyle( (CompositeType) elementType, cascade );
		}
		else {
			return getCascadeStyle( cascade );
		}
	}
	
	private static CascadeStyle getCascadeStyle(String cascade) {
		if ( cascade==null || cascade.equals("none") ) {
			return CascadeStyle.NONE;
		}
		else {
			StringTokenizer tokens = new StringTokenizer(cascade, ", ");
			CascadeStyle[] styles = new CascadeStyle[ tokens.countTokens() ] ;
			int i=0;
			while ( tokens.hasMoreTokens() ) {
				styles[i++] = CascadeStyle.getCascadeStyle( tokens.nextToken() );
			}
			return new CascadeStyle.MultipleCascadeStyle(styles);
		}		
	}
	
	public String getCascade() {
		return cascade;
	}

	public void setCascade(String cascade) {
		this.cascade = cascade;
	}

	public void setName(String name) {
		this.name = name==null ? null : name.intern();
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public boolean isUpdateable() {
		// if the property mapping consists of all formulas,
		// make it non-updateable
		return updateable && !ArrayHelper.isAllFalse( value.getColumnUpdateability() );
	}

	public boolean isInsertable() {
		// if the property mapping consists of all formulas, 
		// make it non-insertable
		final boolean[] columnInsertability = value.getColumnInsertability();
		return insertable && (
				columnInsertability.length==0 ||
				!ArrayHelper.isAllFalse( columnInsertability )
			);
	}

    public PropertyGeneration getGeneration() {
        return generation;
    }

    public void setGeneration(PropertyGeneration generation) {
        this.generation = generation;
    }

    public void setUpdateable(boolean mutable) {
		this.updateable = mutable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	public void setPropertyAccessorName(String string) {
		propertyAccessorName = string;
	}

	/**
	 * Approximate!
	 */
	boolean isNullable() {
		return value==null || value.isNullable();
	}

	public boolean isBasicPropertyAccessor() {
		return propertyAccessorName==null || "property".equals(propertyAccessorName);
	}

	public java.util.Map getMetaAttributes() {
		return metaAttributes;
	}

	public MetaAttribute getMetaAttribute(String attributeName) {
		return metaAttributes==null?null:(MetaAttribute) metaAttributes.get(attributeName);
	}

	public void setMetaAttributes(java.util.Map metas) {
		this.metaAttributes = metas;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		return getValue().isValid(mapping);
	}

	public String toString() {
		return getClass().getName() + '(' + name + ')';
	}
	
	public void setLazy(boolean lazy) {
		this.lazy=lazy;
	}
	
	public boolean isLazy() {
		if ( value instanceof ToOne ) {
			// both many-to-one and one-to-one are represented as a
			// Property.  EntityPersister is relying on this value to
			// determine "lazy fetch groups" in terms of field-level
			// interception.  So we need to make sure that we return
			// true here for the case of many-to-one and one-to-one
			// with lazy="no-proxy"
			//
			// * impl note - lazy="no-proxy" currently forces both
			// lazy and unwrap to be set to true.  The other case we
			// are extremely interested in here is that of lazy="proxy"
			// where lazy is set to true, but unwrap is set to false.
			// thus we use both here under the assumption that this
			// return is really only ever used during persister
			// construction to determine the lazy property/field fetch
			// groupings.  If that assertion changes then this check
			// needs to change as well.  Partially, this is an issue with
			// the overloading of the term "lazy" here...
			ToOne toOneValue = ( ToOne ) value;
			return toOneValue.isLazy() && toOneValue.isUnwrapProxy();
		}
		return lazy;
	}
	
	public boolean isOptimisticLocked() {
		return optimisticLocked;
	}

	public void setOptimisticLocked(boolean optimisticLocked) {
		this.optimisticLocked = optimisticLocked;
	}
	
	public boolean isOptional() {
		return optional || isNullable();
	}
	
	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	public boolean isSelectable() {
		return selectable;
	}
	
	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getAccessorPropertyName( EntityMode mode ) {
		return getName();
	}

	// todo : remove
	public Getter getGetter(Class clazz) throws PropertyNotFoundException, MappingException {
		return getPropertyAccessor(clazz).getGetter( clazz, name );
	}

	// todo : remove
	public Setter getSetter(Class clazz) throws PropertyNotFoundException, MappingException {
		return getPropertyAccessor(clazz).getSetter(clazz, name);
	}

	// todo : remove
	public PropertyAccessor getPropertyAccessor(Class clazz) throws MappingException {
		return PropertyAccessorFactory.getPropertyAccessor( clazz, getPropertyAccessorName() );
	}

	public boolean isNaturalIdentifier() {
		return naturalIdentifier;
	}

	public void setNaturalIdentifier(boolean naturalIdentifier) {
		this.naturalIdentifier = naturalIdentifier;
	}

	public boolean isLob() {
		return lob;
	}

	public void setLob(boolean lob) {
		this.lob = lob;
	}

}
