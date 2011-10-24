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

import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.binding.AttributeBinding;

/**
 * A factory for building/retrieving PropertyAccessor instances.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class PropertyAccessorFactory {

	private static final PropertyAccessor BASIC_PROPERTY_ACCESSOR = new BasicPropertyAccessor();
	private static final PropertyAccessor DIRECT_PROPERTY_ACCESSOR = new DirectPropertyAccessor();
	private static final PropertyAccessor MAP_ACCESSOR = new MapAccessor();
	private static final PropertyAccessor NOOP_ACCESSOR = new NoopAccessor();
	private static final PropertyAccessor EMBEDDED_PROPERTY_ACCESSOR = new EmbeddedPropertyAccessor();

	//TODO: ideally we need the construction of PropertyAccessor to take the following:
	//      1) EntityMode
	//      2) EntityMode-specific data (i.e., the classname for pojo entities)
	//      3) Property-specific data based on the EntityMode (i.e., property-name or dom4j-node-name)
	// The easiest way, with the introduction of the new runtime-metamodel classes, would be the
	// the following predicates:
	//      1) PropertyAccessorFactory.getPropertyAccessor() takes references to both a
	//          org.hibernate.metadata.EntityModeMetadata and org.hibernate.metadata.Property
	//      2) What is now termed a "PropertyAccessor" stores any values needed from those two
	//          pieces of information
	//      3) Code can then simply call PropertyAccess.getGetter() with no parameters; likewise with
	//          PropertyAccessor.getSetter()

    /**
     * Retrieves a PropertyAccessor instance based on the given property definition and
     * entity mode.
     *
     * @param property The property for which to retrieve an accessor.
     * @param mode The mode for the resulting entity.
     * @return An appropriate accessor.
     * @throws MappingException
     */
	public static PropertyAccessor getPropertyAccessor(Property property, EntityMode mode) throws MappingException {
		//TODO: this is temporary in that the end result will probably not take a Property reference per-se.
	    if ( null == mode || EntityMode.POJO.equals( mode ) ) {
		    return getPojoPropertyAccessor( property.getPropertyAccessorName() );
	    }
	    else if ( EntityMode.MAP.equals( mode ) ) {
		    return getDynamicMapPropertyAccessor();
	    }
	    else {
		    throw new MappingException( "Unknown entity mode [" + mode + "]" );
	    }
	}

	/**
     * Retrieves a PropertyAccessor instance based on the given property definition and
     * entity mode.
     *
     * @param property The property for which to retrieve an accessor.
     * @param mode The mode for the resulting entity.
     * @return An appropriate accessor.
     * @throws MappingException
     */
	public static PropertyAccessor getPropertyAccessor(AttributeBinding property, EntityMode mode) throws MappingException {
		//TODO: this is temporary in that the end result will probably not take a Property reference per-se.
	    if ( null == mode || EntityMode.POJO.equals( mode ) ) {
		    return getPojoPropertyAccessor( property.getPropertyAccessorName() );
	    }
	    else if ( EntityMode.MAP.equals( mode ) ) {
		    return getDynamicMapPropertyAccessor();
	    }
	    else {
		    throw new MappingException( "Unknown entity mode [" + mode + "]" );
	    }
	}

	/**
	 * Retreives a PropertyAccessor specific for a PojoRepresentation with the given access strategy.
	 *
	 * @param pojoAccessorStrategy The access strategy.
	 * @return An appropriate accessor.
	 */
	private static PropertyAccessor getPojoPropertyAccessor(String pojoAccessorStrategy) {
		if ( StringHelper.isEmpty( pojoAccessorStrategy ) || "property".equals( pojoAccessorStrategy ) ) {
			return BASIC_PROPERTY_ACCESSOR;
		}
		else if ( "field".equals( pojoAccessorStrategy ) ) {
			return DIRECT_PROPERTY_ACCESSOR;
		}
		else if ( "embedded".equals( pojoAccessorStrategy ) ) {
			return EMBEDDED_PROPERTY_ACCESSOR;
		}
		else if ( "noop".equals(pojoAccessorStrategy) ) {
			return NOOP_ACCESSOR;
		}
		else {
			return resolveCustomAccessor( pojoAccessorStrategy );
		}
	}

	public static PropertyAccessor getDynamicMapPropertyAccessor() throws MappingException {
		return MAP_ACCESSOR;
	}

	private static PropertyAccessor resolveCustomAccessor(String accessorName) {
		Class accessorClass;
		try {
			accessorClass = ReflectHelper.classForName( accessorName );
		}
		catch (ClassNotFoundException cnfe) {
			throw new MappingException("could not find PropertyAccessor class: " + accessorName, cnfe);
		}
		try {
			return (PropertyAccessor) accessorClass.newInstance();
		}
		catch (Exception e) {
			throw new MappingException("could not instantiate PropertyAccessor class: " + accessorName, e);
		}
	}

	private PropertyAccessorFactory() {}

	// todo : this eventually needs to be removed
	public static PropertyAccessor getPropertyAccessor(Class optionalClass, String type) throws MappingException {
		if ( type==null ) type = optionalClass==null || optionalClass==Map.class ? "map" : "property";
		return getPropertyAccessor(type);
	}

	// todo : this eventually needs to be removed
	public static PropertyAccessor getPropertyAccessor(String type) throws MappingException {
		if ( type==null || "property".equals(type) ) return BASIC_PROPERTY_ACCESSOR;
		if ( "field".equals(type) ) return DIRECT_PROPERTY_ACCESSOR;
		if ( "map".equals(type) ) return MAP_ACCESSOR;
		if ( "embedded".equals(type) ) return EMBEDDED_PROPERTY_ACCESSOR;
		if ( "noop".equals(type)) return NOOP_ACCESSOR;

		return resolveCustomAccessor(type);
	}
}
