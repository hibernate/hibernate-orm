/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

// $Id$

package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

import org.jboss.logging.Logger;

import jakarta.persistence.Access;
import jakarta.persistence.Basic;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

/**
 * A helper class to keep the {@code XProperty}s of a class ordered by access type.
 *
 * @author Hardy Ferentschik
 */
public class PropertyContainer {

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

	private final List<XProperty> persistentAttributes;

	public PropertyContainer(XClass clazz, XClass entityAtStake, AccessType defaultClassLevelAccessType) {
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
		assert classLevelAccessType == AccessType.FIELD || classLevelAccessType == AccessType.PROPERTY
				|| classLevelAccessType == AccessType.RECORD;


		final List<XProperty> fields = xClass.getDeclaredProperties( AccessType.FIELD.getType() );
		final List<XProperty> getters = xClass.getDeclaredProperties( AccessType.PROPERTY.getType() );
		final List<XProperty> recordComponents = xClass.getDeclaredProperties( AccessType.RECORD.getType() );

		preFilter( fields, getters, recordComponents );

		final Map<String,XProperty> persistentAttributesFromGetters = new HashMap<>();
		final Map<String,XProperty> persistentAttributesFromComponents = new HashMap<>();

		final Map<String, XProperty> localAttributeMap;
		// If the record class has only record components which match up with fields and no additional getters,
		// we can retain the property order, to match up with the record component order
		if ( !recordComponents.isEmpty() && recordComponents.size() == fields.size() && getters.isEmpty() ) {
			localAttributeMap = new LinkedHashMap<>();
		}
		//otherwise we sort them in alphabetical order, since this is at least deterministic
		else {
			localAttributeMap = new TreeMap<>();
		}
		collectPersistentAttributesUsingLocalAccessType(
				xClass,
				localAttributeMap,
				persistentAttributesFromGetters,
				persistentAttributesFromComponents,
				fields,
				getters,
				recordComponents
		);
		collectPersistentAttributesUsingClassLevelAccessType(
				xClass,
				classLevelAccessType,
				localAttributeMap,
				persistentAttributesFromGetters,
				persistentAttributesFromComponents,
				fields,
				getters,
				recordComponents
		);
		this.persistentAttributes = verifyAndInitializePersistentAttributes( xClass, localAttributeMap );
	}

	private void preFilter(List<XProperty> fields, List<XProperty> getters, List<XProperty> recordComponents) {
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

		propertyIterator = recordComponents.iterator();
		while ( propertyIterator.hasNext() ) {
			final XProperty property = propertyIterator.next();
			if ( mustBeSkipped( property ) ) {
				propertyIterator.remove();
			}
		}
	}

	private static void collectPersistentAttributesUsingLocalAccessType(
			XClass xClass,
			Map<String, XProperty> persistentAttributeMap,
			Map<String,XProperty> persistentAttributesFromGetters,
			Map<String,XProperty> persistentAttributesFromComponents,
			List<XProperty> fields,
			List<XProperty> getters,
			List<XProperty> recordComponents) {

		// Check fields...
		Iterator<XProperty> propertyIterator = fields.iterator();
		while ( propertyIterator.hasNext() ) {
			final XProperty xProperty = propertyIterator.next();
			final Access localAccessAnnotation = xProperty.getAnnotation( Access.class );
			if ( localAccessAnnotation == null
					|| localAccessAnnotation.value() != jakarta.persistence.AccessType.FIELD ) {
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
					|| localAccessAnnotation.value() != jakarta.persistence.AccessType.PROPERTY ) {
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

		// Check record components...
		propertyIterator = recordComponents.iterator();
		while ( propertyIterator.hasNext() ) {
			final XProperty xProperty = propertyIterator.next();
			final Access localAccessAnnotation = xProperty.getAnnotation( Access.class );
			if ( localAccessAnnotation == null ) {
				continue;
			}

			propertyIterator.remove();
			final String name = xProperty.getName();
			persistentAttributeMap.put( name, xProperty );
			persistentAttributesFromComponents.put( name, xProperty );
		}
	}

	private static void collectPersistentAttributesUsingClassLevelAccessType(
			XClass xClass,
			AccessType classLevelAccessType,
			Map<String, XProperty> persistentAttributeMap,
			Map<String,XProperty> persistentAttributesFromGetters,
			Map<String,XProperty> persistentAttributesFromComponents,
			List<XProperty> fields,
			List<XProperty> getters,
			List<XProperty> recordComponents) {
		if ( classLevelAccessType == AccessType.FIELD ) {
			for ( XProperty field : fields ) {
				final String name = field.getName();
				if ( persistentAttributeMap.containsKey( name ) ) {
					continue;
				}

				persistentAttributeMap.put( name, field );
			}
		}
		else {
			for ( XProperty getter : getters ) {
				final String name = getter.getName();

				// HHH-10242 detect registration of the same property getter twice - eg boolean isId() + UUID getId()
				final XProperty previous = persistentAttributesFromGetters.get( name );
				if ( previous != null ) {
					throw new MappingException(
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
			// When a user uses the `property` access strategy for the entity owning an embeddable,
			// we also have to add the attributes for record components,
			// because record classes usually don't have getters, but just the record component accessors
			for ( XProperty recordComponent : recordComponents ) {
				final String name = recordComponent.getName();
				if ( persistentAttributeMap.containsKey( name ) ) {
					continue;
				}

				persistentAttributeMap.put( name, recordComponent );
				persistentAttributesFromComponents.put( name, recordComponent );
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

	public Iterable<XProperty> propertyIterator() {
		return persistentAttributes;
	}

	private static List<XProperty> verifyAndInitializePersistentAttributes(XClass xClass, Map<String, XProperty> localAttributeMap) {
		ArrayList<XProperty> output = new ArrayList<>( localAttributeMap.size() );
		for ( XProperty xProperty : localAttributeMap.values() ) {
			if ( !xProperty.isTypeResolved() && !discoverTypeWithoutReflection( xClass, xProperty ) ) {
				String msg = "Property '" + StringHelper.qualify( xClass.getName(), xProperty.getName() ) +
						"' has an unbound type and no explicit target entity (resolve this generics usage issue" +
						" or set an explicit target attribute with '@OneToMany(target=)' or use an explicit '@Type')";
				throw new AnnotationException( msg );
			}
			output.add( xProperty );
		}
		return CollectionHelper.toSmallList( output );
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
		AccessType classDefinedAccessType = AccessType.DEFAULT;
		Access access = xClass.getAnnotation( Access.class );
		if ( access != null ) {
			classDefinedAccessType = AccessType.getAccessStrategy( access.value() );
		}
		return classDefinedAccessType;
	}

	private static boolean discoverTypeWithoutReflection(XClass clazz, XProperty property) {
		if ( property.isAnnotationPresent( OneToOne.class ) && !property.getAnnotation( OneToOne.class )
				.targetEntity()
				.equals( void.class ) ) {
			return true;
		}
		else if ( property.isAnnotationPresent( OneToMany.class ) && !property.getAnnotation( OneToMany.class )
				.targetEntity()
				.equals( void.class ) ) {
			return true;
		}
		else if ( property.isAnnotationPresent( ManyToOne.class ) && !property.getAnnotation( ManyToOne.class )
				.targetEntity()
				.equals( void.class ) ) {
			return true;
		}
		else if ( property.isAnnotationPresent( ManyToMany.class ) && !property.getAnnotation( ManyToMany.class )
				.targetEntity()
				.equals( void.class ) ) {
			return true;
		}
		else if ( property.isAnnotationPresent( org.hibernate.annotations.Any.class ) ) {
			return true;
		}
		else if ( property.isAnnotationPresent( ManyToAny.class ) ) {
			if ( !property.isCollection() && !property.isArray() ) {
				throw new AnnotationException( "Property '" + StringHelper.qualify( clazz.getName(), property.getName() )
						+ "' annotated '@ManyToAny' is neither a collection nor an array" );
			}
			return true;
		}
		else if ( property.isAnnotationPresent( Basic.class ) ) {
			return true;
		}
		else if ( property.isAnnotationPresent( Type.class ) ) {
			return true;
		}
		else if ( property.isAnnotationPresent( JavaType.class ) ) {
			return true;
		}
		else if ( property.isAnnotationPresent( JdbcTypeCode.class ) ) {
			return true;
		}
		else if ( property.isAnnotationPresent( Target.class ) ) {
			return true;
		}
		return false;
	}

	private static boolean mustBeSkipped(XProperty property) {
		//TODO make those hardcoded tests more portable (through the bytecode provider?)
		return property.isAnnotationPresent( Transient.class )
				|| "net.sf.cglib.transform.impl.InterceptFieldCallback".equals( property.getType().getName() );
	}
}
