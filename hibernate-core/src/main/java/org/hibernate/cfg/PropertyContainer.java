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

// $Id$

package org.hibernate.cfg;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.persistence.Access;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

/**
 * A helper class to keep the {@code XProperty}s of a class ordered by access type.
 *
 * @author Hardy Ferentschik
 */
class PropertyContainer {

    static {
        System.setProperty("jboss.i18n.generate-proxies", "true");
    }

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, PropertyContainer.class.getName());

	private final AccessType explicitClassDefinedAccessType;

	/**
	 * Constains the properties which must be returned in case the class is accessed via {@code AccessType.FIELD}. Note,
	 * this does not mean that all {@code XProperty}s in this map are fields. Due to JPA access rules single properties
	 * can have different access type than the overall class access type.
	 */
	private final TreeMap<String, XProperty> fieldAccessMap;

	/**
	 * Constains the properties which must be returned in case the class is accessed via {@code AccessType.Property}. Note,
	 * this does not mean that all {@code XProperty}s in this map are properties/methods. Due to JPA access rules single properties
	 * can have different access type than the overall class access type.
	 */
	private final TreeMap<String, XProperty> propertyAccessMap;

	/**
	 * The class for which this container is created.
	 */
	private final XClass xClass;
	private final XClass entityAtStake;

	PropertyContainer(XClass clazz, XClass entityAtStake) {
		this.xClass = clazz;
		this.entityAtStake = entityAtStake;

		explicitClassDefinedAccessType = determineClassDefinedAccessStrategy();

		// first add all properties to field and property map
		fieldAccessMap = initProperties( AccessType.FIELD );
		propertyAccessMap = initProperties( AccessType.PROPERTY );

		considerExplicitFieldAndPropertyAccess();
	}

	public XClass getEntityAtStake() {
		return entityAtStake;
	}

	public XClass getDeclaringClass() {
		return xClass;
	}

	public AccessType getExplicitAccessStrategy() {
		return explicitClassDefinedAccessType;
	}

	public boolean hasExplicitAccessStrategy() {
		return !explicitClassDefinedAccessType.equals( AccessType.DEFAULT );
	}

	public Collection<XProperty> getProperties(AccessType accessType) {
		assertTypesAreResolvable( accessType );
		if ( AccessType.DEFAULT == accessType || AccessType.PROPERTY == accessType ) {
			return Collections.unmodifiableCollection( propertyAccessMap.values() );
		}
		else {
			return Collections.unmodifiableCollection( fieldAccessMap.values() );
		}
	}

	private void assertTypesAreResolvable(AccessType access) {
		Map<String, XProperty> xprops;
		if ( AccessType.PROPERTY.equals( access ) || AccessType.DEFAULT.equals( access ) ) {
			xprops = propertyAccessMap;
		}
		else {
			xprops = fieldAccessMap;
		}
		for ( XProperty property : xprops.values() ) {
			if ( !property.isTypeResolved() && !discoverTypeWithoutReflection( property ) ) {
				String msg = "Property " + StringHelper.qualify( xClass.getName(), property.getName() ) +
						" has an unbound type and no explicit target entity. Resolve this Generic usage issue" +
						" or set an explicit target attribute (eg @OneToMany(target=) or use an explicit @Type";
				throw new AnnotationException( msg );
			}
		}
	}

	private void considerExplicitFieldAndPropertyAccess() {
		for ( XProperty property : fieldAccessMap.values() ) {
			Access access = property.getAnnotation( Access.class );
			if ( access == null ) {
				continue;
			}

			// see "2.3.2 Explicit Access Type" of JPA 2 spec
			// the access type for this property is explicitly set to AccessType.FIELD, hence we have to
			// use field access for this property even if the default access type for the class is AccessType.PROPERTY
			AccessType accessType = AccessType.getAccessStrategy( access.value() );
            if (accessType == AccessType.FIELD) {
				propertyAccessMap.put(property.getName(), property);
			}
            else {
				LOG.debug( "Placing @Access(AccessType.FIELD) on a field does not have any effect." );
			}
		}

		for ( XProperty property : propertyAccessMap.values() ) {
			Access access = property.getAnnotation( Access.class );
			if ( access == null ) {
				continue;
			}

			AccessType accessType = AccessType.getAccessStrategy( access.value() );

			// see "2.3.2 Explicit Access Type" of JPA 2 spec
			// the access type for this property is explicitly set to AccessType.PROPERTY, hence we have to
			// return use method access even if the default class access type is AccessType.FIELD
            if (accessType == AccessType.PROPERTY) {
				fieldAccessMap.put(property.getName(), property);
			}
            else {
				LOG.debug( "Placing @Access(AccessType.PROPERTY) on a field does not have any effect." );
			}
		}
	}

	/**
	 * Retrieves all properties from the {@code xClass} with the specified access type. This method does not take
	 * any jpa access rules/annotations into account yet.
	 *
	 * @param access The access type - {@code AccessType.FIELD}  or {@code AccessType.Property}
	 *
	 * @return A maps of the properties with the given access type keyed against their property name
	 */
	private TreeMap<String, XProperty> initProperties(AccessType access) {
		if ( !( AccessType.PROPERTY.equals( access ) || AccessType.FIELD.equals( access ) ) ) {
			throw new IllegalArgumentException( "Access type has to be AccessType.FIELD or AccessType.Property" );
		}

		//order so that property are used in the same order when binding native query
		TreeMap<String, XProperty> propertiesMap = new TreeMap<String, XProperty>();
		List<XProperty> properties = xClass.getDeclaredProperties( access.getType() );
		for ( XProperty property : properties ) {
			if ( mustBeSkipped( property ) ) {
				continue;
			}
			propertiesMap.put( property.getName(), property );
		}
		return propertiesMap;
	}

	private AccessType determineClassDefinedAccessStrategy() {
		AccessType classDefinedAccessType;

		AccessType hibernateDefinedAccessType = AccessType.DEFAULT;
		AccessType jpaDefinedAccessType = AccessType.DEFAULT;

		org.hibernate.annotations.AccessType accessType = xClass.getAnnotation( org.hibernate.annotations.AccessType.class );
		if ( accessType != null ) {
			hibernateDefinedAccessType = AccessType.getAccessStrategy( accessType.value() );
		}

		Access access = xClass.getAnnotation( Access.class );
		if ( access != null ) {
			jpaDefinedAccessType = AccessType.getAccessStrategy( access.value() );
		}

		if ( hibernateDefinedAccessType != AccessType.DEFAULT
				&& jpaDefinedAccessType != AccessType.DEFAULT
				&& hibernateDefinedAccessType != jpaDefinedAccessType ) {
			throw new MappingException(
					"@AccessType and @Access specified with contradicting values. Use of @Access only is recommended. "
			);
		}

		if ( hibernateDefinedAccessType != AccessType.DEFAULT ) {
			classDefinedAccessType = hibernateDefinedAccessType;
		}
		else {
			classDefinedAccessType = jpaDefinedAccessType;
		}
		return classDefinedAccessType;
	}

	private static boolean discoverTypeWithoutReflection(XProperty p) {
		if ( p.isAnnotationPresent( OneToOne.class ) && !p.getAnnotation( OneToOne.class )
				.targetEntity()
				.equals( void.class ) ) {
			return true;
		}
		else if ( p.isAnnotationPresent( OneToMany.class ) && !p.getAnnotation( OneToMany.class )
				.targetEntity()
				.equals( void.class ) ) {
			return true;
		}
		else if ( p.isAnnotationPresent( ManyToOne.class ) && !p.getAnnotation( ManyToOne.class )
				.targetEntity()
				.equals( void.class ) ) {
			return true;
		}
		else if ( p.isAnnotationPresent( ManyToMany.class ) && !p.getAnnotation( ManyToMany.class )
				.targetEntity()
				.equals( void.class ) ) {
			return true;
		}
		else if ( p.isAnnotationPresent( org.hibernate.annotations.Any.class ) ) {
			return true;
		}
		else if ( p.isAnnotationPresent( ManyToAny.class ) ) {
			if ( !p.isCollection() && !p.isArray() ) {
				throw new AnnotationException( "@ManyToAny used on a non collection non array property: " + p.getName() );
			}
			return true;
		}
		else if ( p.isAnnotationPresent( Type.class ) ) {
			return true;
		}
		else if ( p.isAnnotationPresent( Target.class ) ) {
			return true;
		}
		return false;
	}

	private static boolean mustBeSkipped(XProperty property) {
		//TODO make those hardcoded tests more portable (through the bytecode provider?)
		return property.isAnnotationPresent( Transient.class )
				|| "net.sf.cglib.transform.impl.InterceptFieldCallback".equals( property.getType().getName() )
				|| "org.hibernate.bytecode.internal.javassist.FieldHandler".equals( property.getType().getName() );
	}
}


