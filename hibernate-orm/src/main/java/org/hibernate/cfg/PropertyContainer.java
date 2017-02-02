/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

package org.hibernate.cfg;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.persistence.Access;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.cfg.annotations.HCANNHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

/**
 * A helper class to keep the {@code XProperty}s of a class ordered by access type.
 *
 * @author Hardy Ferentschik
 */
class PropertyContainer {
//
//    static {
//        System.setProperty("jboss.i18n.generate-proxies", "true");
//    }

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, PropertyContainer.class.getName());

	/**
	 * The class for which this container is created.
	 */
	private final XClass xClass;
	private final XClass entityAtStake;

	/**
	 * Holds the AccessType indicated for use at the class/container-level for cases where persistent attribute
	 * did not specify.
	 */
	private final AccessType classLevelAccessType;

	private final TreeMap<String, XProperty> persistentAttributeMap;

	PropertyContainer(XClass clazz, XClass entityAtStake, AccessType defaultClassLevelAccessType) {
		this.xClass = clazz;
		this.entityAtStake = entityAtStake;

		if ( defaultClassLevelAccessType == AccessType.DEFAULT ) {
			// this is effectively what the old code did when AccessType.DEFAULT was passed in
			// to getProperties(AccessType) from AnnotationBinder and InheritanceState
			defaultClassLevelAccessType = AccessType.PROPERTY;
		}

		AccessType localClassLevelAccessType = determineLocalClassDefinedAccessStrategy();
		assert localClassLevelAccessType != null;

		this.classLevelAccessType = localClassLevelAccessType != AccessType.DEFAULT
				? localClassLevelAccessType
				: defaultClassLevelAccessType;
		assert classLevelAccessType == AccessType.FIELD || classLevelAccessType == AccessType.PROPERTY;

		this.persistentAttributeMap = new TreeMap<String, XProperty>();

		final List<XProperty> fields = xClass.getDeclaredProperties( AccessType.FIELD.getType() );
		final List<XProperty> getters = xClass.getDeclaredProperties( AccessType.PROPERTY.getType() );

		preFilter( fields, getters );

		final Map<String,XProperty> persistentAttributesFromGetters = new HashMap<String, XProperty>();

		collectPersistentAttributesUsingLocalAccessType(
				persistentAttributeMap,
				persistentAttributesFromGetters,
				fields,
				getters
		);
		collectPersistentAttributesUsingClassLevelAccessType(
				persistentAttributeMap,
				persistentAttributesFromGetters,
				fields,
				getters
		);
	}

	private void preFilter(List<XProperty> fields, List<XProperty> getters) {
		Iterator<XProperty> propertyIterator = fields.iterator();
		while ( propertyIterator.hasNext() ) {
			final XProperty property = propertyIterator.next();
			if ( mustBeSkipped( property ) ) {
				propertyIterator.remove();
			}
		}

		propertyIterator = getters.iterator();
		while ( propertyIterator.hasNext() ) {
			final XProperty property = propertyIterator.next();
			if ( mustBeSkipped( property ) ) {
				propertyIterator.remove();
			}
		}
	}

	private void collectPersistentAttributesUsingLocalAccessType(
			TreeMap<String, XProperty> persistentAttributeMap,
			Map<String,XProperty> persistentAttributesFromGetters,
			List<XProperty> fields,
			List<XProperty> getters) {

		// Check fields...
		Iterator<XProperty> propertyIterator = fields.iterator();
		while ( propertyIterator.hasNext() ) {
			final XProperty xProperty = propertyIterator.next();
			final Access localAccessAnnotation = xProperty.getAnnotation( Access.class );
			if ( localAccessAnnotation == null
					|| localAccessAnnotation.value() != javax.persistence.AccessType.FIELD ) {
				continue;
			}

			propertyIterator.remove();
			persistentAttributeMap.put( xProperty.getName(), xProperty );
		}

		// Check getters...
		propertyIterator = getters.iterator();
		while ( propertyIterator.hasNext() ) {
			final XProperty xProperty = propertyIterator.next();
			final Access localAccessAnnotation = xProperty.getAnnotation( Access.class );
			if ( localAccessAnnotation == null
					|| localAccessAnnotation.value() != javax.persistence.AccessType.PROPERTY ) {
				continue;
			}

			propertyIterator.remove();

			final String name = xProperty.getName();

			// HHH-10242 detect registration of the same property getter twice - eg boolean isId() + UUID getId()
			final XProperty previous = persistentAttributesFromGetters.get( name );
			if ( previous != null ) {
				throw new org.hibernate.boot.MappingException(
						LOG.ambiguousPropertyMethods(
								xClass.getName(),
								HCANNHelper.annotatedElementSignature( previous ),
								HCANNHelper.annotatedElementSignature( xProperty )
						),
						new Origin( SourceType.ANNOTATION, xClass.getName() )
				);
			}

			persistentAttributeMap.put( name, xProperty );
			persistentAttributesFromGetters.put( name, xProperty );
		}
	}

	private void collectPersistentAttributesUsingClassLevelAccessType(
			TreeMap<String, XProperty> persistentAttributeMap,
			Map<String,XProperty> persistentAttributesFromGetters,
			List<XProperty> fields,
			List<XProperty> getters) {
		if ( classLevelAccessType == AccessType.FIELD ) {
			for ( XProperty field : fields ) {
				if ( persistentAttributeMap.containsKey( field.getName() ) ) {
					continue;
				}

				persistentAttributeMap.put( field.getName(), field );
			}
		}
		else {
			for ( XProperty getter : getters ) {
				final String name = getter.getName();

				// HHH-10242 detect registration of the same property getter twice - eg boolean isId() + UUID getId()
				final XProperty previous = persistentAttributesFromGetters.get( name );
				if ( previous != null ) {
					throw new org.hibernate.boot.MappingException(
							LOG.ambiguousPropertyMethods(
									xClass.getName(),
									HCANNHelper.annotatedElementSignature( previous ),
									HCANNHelper.annotatedElementSignature( getter )
							),
							new Origin( SourceType.ANNOTATION, xClass.getName() )
					);
				}

				if ( persistentAttributeMap.containsKey( name ) ) {
					continue;
				}

				persistentAttributeMap.put( getter.getName(), getter );
				persistentAttributesFromGetters.put( name, getter );
			}
		}
	}

	public XClass getEntityAtStake() {
		return entityAtStake;
	}

	public XClass getDeclaringClass() {
		return xClass;
	}

	public AccessType getClassLevelAccessType() {
		return classLevelAccessType;
	}

	public Collection<XProperty> getProperties() {
		assertTypesAreResolvable();
		return Collections.unmodifiableCollection( persistentAttributeMap.values() );
	}

	private void assertTypesAreResolvable() {
		for ( XProperty xProperty : persistentAttributeMap.values() ) {
			if ( !xProperty.isTypeResolved() && !discoverTypeWithoutReflection( xProperty ) ) {
				String msg = "Property " + StringHelper.qualify( xClass.getName(), xProperty.getName() ) +
						" has an unbound type and no explicit target entity. Resolve this Generic usage issue" +
						" or set an explicit target attribute (eg @OneToMany(target=) or use an explicit @Type";
				throw new AnnotationException( msg );
			}
		}
	}
//
//	private void considerExplicitFieldAndPropertyAccess() {
//		for ( XProperty property : fieldAccessMap.values() ) {
//			Access access = property.getAnnotation( Access.class );
//			if ( access == null ) {
//				continue;
//			}
//
//			// see "2.3.2 Explicit Access Type" of JPA 2 spec
//			// the access type for this property is explicitly set to AccessType.FIELD, hence we have to
//			// use field access for this property even if the default access type for the class is AccessType.PROPERTY
//			AccessType accessType = AccessType.getAccessStrategy( access.value() );
//            if (accessType == AccessType.FIELD) {
//				propertyAccessMap.put(property.getName(), property);
//			}
//            else {
//				LOG.debug( "Placing @Access(AccessType.FIELD) on a field does not have any effect." );
//			}
//		}
//
//		for ( XProperty property : propertyAccessMap.values() ) {
//			Access access = property.getAnnotation( Access.class );
//			if ( access == null ) {
//				continue;
//			}
//
//			AccessType accessType = AccessType.getAccessStrategy( access.value() );
//
//			// see "2.3.2 Explicit Access Type" of JPA 2 spec
//			// the access type for this property is explicitly set to AccessType.PROPERTY, hence we have to
//			// return use method access even if the default class access type is AccessType.FIELD
//            if (accessType == AccessType.PROPERTY) {
//				fieldAccessMap.put(property.getName(), property);
//			}
//            else {
//				LOG.debug( "Placing @Access(AccessType.PROPERTY) on a field does not have any effect." );
//			}
//		}
//	}

//	/**
//	 * Retrieves all properties from the {@code xClass} with the specified access type. This method does not take
//	 * any jpa access rules/annotations into account yet.
//	 *
//	 * @param access The access type - {@code AccessType.FIELD}  or {@code AccessType.Property}
//	 *
//	 * @return A maps of the properties with the given access type keyed against their property name
//	 */
//	private TreeMap<String, XProperty> initProperties(AccessType access) {
//		if ( !( AccessType.PROPERTY.equals( access ) || AccessType.FIELD.equals( access ) ) ) {
//			throw new IllegalArgumentException( "Access type has to be AccessType.FIELD or AccessType.Property" );
//		}
//
//		//order so that property are used in the same order when binding native query
//		TreeMap<String, XProperty> propertiesMap = new TreeMap<String, XProperty>();
//		List<XProperty> properties = xClass.getDeclaredProperties( access.getType() );
//		for ( XProperty property : properties ) {
//			if ( mustBeSkipped( property ) ) {
//				continue;
//			}
//			// HHH-10242 detect registration of the same property twice eg boolean isId() + UUID getId()
//			XProperty oldProperty = propertiesMap.get( property.getName() );
//			if ( oldProperty != null ) {
//				throw new org.hibernate.boot.MappingException(
//						LOG.ambiguousPropertyMethods(
//								xClass.getName(),
//								HCANNHelper.annotatedElementSignature( oldProperty ),
//								HCANNHelper.annotatedElementSignature( property )
//						),
//						new Origin( SourceType.ANNOTATION, xClass.getName() )
//				);
//			}
//
//			propertiesMap.put( property.getName(), property );
//		}
//		return propertiesMap;
//	}

	private AccessType determineLocalClassDefinedAccessStrategy() {
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
