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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.Type;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.RecordComponentDetails;
import org.hibernate.models.spi.TypeDetails;

import org.jboss.logging.Logger;

import jakarta.persistence.Access;
import jakarta.persistence.Basic;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

/**
 * Access to the members of a {@linkplain ClassDetails class} which define a persistent attribute
 * as defined by the JPA specification and AccessType.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class PropertyContainer {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, PropertyContainer.class.getName());

	/**
	 * The class for which this container is created.
	 */
	private final ClassDetails classDetails;
	private final ClassDetails entityAtStake;

	/**
	 * Holds the AccessType indicated for use at the class/container-level for cases where persistent attribute
	 * did not specify.
	 */
	private final AccessType classLevelAccessType;

	private final List<MemberDetails> attributeMembers;

	public PropertyContainer(ClassDetails classDetails, TypeDetails entityAtStake, AccessType propertyAccessor) {
		// todo : should use the TypeDetails, no?
		this( classDetails, entityAtStake.determineRawClass(), propertyAccessor );
	}

	public PropertyContainer(TypeDetails classDetails, TypeDetails entityAtStake, AccessType propertyAccessor) {
		// todo : should use the TypeDetails, no?
		this( classDetails.determineRawClass(), entityAtStake.determineRawClass(), propertyAccessor );
	}

	public PropertyContainer(ClassDetails classDetails, ClassDetails entityAtStake, AccessType defaultClassLevelAccessType) {
		this.classDetails = classDetails;
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

		attributeMembers = resolveAttributeMembers( classDetails, classLevelAccessType );
	}

	private static List<MemberDetails> resolveAttributeMembers(
			ClassDetails classDetails,
			AccessType classLevelAccessType) {
		final List<FieldDetails> fields = collectPotentialAttributeMembers( classDetails.getFields() );
		final List<MethodDetails> getters = collectPotentialAttributeMembers( classDetails.getMethods() );
		final List<RecordComponentDetails> recordComponents = collectPotentialAttributeMembers( classDetails.getRecordComponents() );

		final Map<String, MemberDetails> attributeMemberMap = buildAttributeMemberMap(
				recordComponents,
				fields,
				getters
		);

		final Map<String,MethodDetails> persistentAttributesFromGetters = new HashMap<>();
		final Map<String,RecordComponentDetails> persistentAttributesFromComponents = new HashMap<>();

		collectPersistentAttributesUsingLocalAccessType(
				classDetails,
				attributeMemberMap,
				persistentAttributesFromGetters,
				persistentAttributesFromComponents,
				fields,
				getters,
				recordComponents
		);
		collectPersistentAttributesUsingClassLevelAccessType(
				classDetails,
				classLevelAccessType,
				attributeMemberMap,
				persistentAttributesFromGetters,
				persistentAttributesFromComponents,
				fields,
				getters,
				recordComponents
		);
		return verifyAndInitializePersistentAttributes( classDetails, attributeMemberMap );
	}

	private static Map<String, MemberDetails> buildAttributeMemberMap(
			List<RecordComponentDetails> recordComponents,
			List<FieldDetails> fields,
			List<MethodDetails> getters) {
		final Map<String, MemberDetails> attributeMemberMap;
		// If the record class has only record components which match up with fields and no additional getters,
		// we can retain the property order, to match up with the record component order
		if ( !recordComponents.isEmpty() && recordComponents.size() == fields.size() && getters.isEmpty() ) {
			attributeMemberMap = new LinkedHashMap<>();
		}
		//otherwise we sort them in alphabetical order, since this is at least deterministic
		else {
			attributeMemberMap = new TreeMap<>();
		}
		return attributeMemberMap;
	}

	private static <E extends MemberDetails> List<E> collectPotentialAttributeMembers(List<E> source) {
		final List<E> results = new ArrayList<>();
		for ( int i = 0; i < source.size(); i++ ) {
			final E possible = source.get( i );
			if ( possible.isPersistable() ) {
				if ( !mustBeSkipped( possible ) ) {
					results.add( possible );
				}
			}
		}
		return results;
	}

	/**
	 * Collects members "backing" an attribute based on any local `@Access` annotation
	 */
	private static void collectPersistentAttributesUsingLocalAccessType(
			ClassDetails classDetails,
			Map<String, MemberDetails> persistentAttributeMap,
			Map<String,MethodDetails> persistentAttributesFromGetters,
			Map<String,RecordComponentDetails> persistentAttributesFromComponents,
			List<FieldDetails> fields,
			List<MethodDetails> getters,
			List<RecordComponentDetails> recordComponents) {

		// Check fields...
		for ( int i = 0; i < fields.size(); i++ ) {
			final FieldDetails fieldDetails = fields.get( i );
			final AnnotationUsage<Access> localAccessAnnotation = fieldDetails.getAnnotationUsage( Access.class );
			if ( localAccessAnnotation == null
					|| localAccessAnnotation.getEnum( "value" ) != jakarta.persistence.AccessType.FIELD ) {
				continue;
			}
			persistentAttributeMap.put( fieldDetails.getName(), fieldDetails );
		}

		// Check getters...
		for ( int i = 0; i < getters.size(); i++ ) {
			final MethodDetails getterDetails = getters.get( i );
			final AnnotationUsage<Access> localAccessAnnotation = getterDetails.getAnnotationUsage( Access.class );
			if ( localAccessAnnotation == null
					|| localAccessAnnotation.getEnum( "value" ) != jakarta.persistence.AccessType.PROPERTY ) {
				continue;
			}

			final String name = getterDetails.resolveAttributeName();

			// HHH-10242 detect registration of the same property getter twice - eg boolean isId() + UUID getId()
			final MethodDetails previous = persistentAttributesFromGetters.get( name );
			if ( previous != null ) {
				throw new org.hibernate.boot.MappingException(
						LOG.ambiguousPropertyMethods(
								classDetails.getName(),
								previous.getName(),
								getterDetails.getName()
						),
						new Origin( SourceType.ANNOTATION, classDetails.getName() )
				);
			}

			persistentAttributeMap.put( name, getterDetails );
			persistentAttributesFromGetters.put( name, getterDetails );
		}

		// Check record components...
		for ( int i = 0; i < recordComponents.size(); i++ ) {
			final RecordComponentDetails componentDetails = recordComponents.get( i );
			final AnnotationUsage<Access> localAccessAnnotation = componentDetails.getAnnotationUsage( Access.class );
			if ( localAccessAnnotation == null ) {
				continue;
			}
			final String name = componentDetails.getName();
			persistentAttributeMap.put( name, componentDetails );
			persistentAttributesFromComponents.put( name, componentDetails );
		}
	}

	/**
	 * Collects members "backing" an attribute based on the Class's "default" access-type
	 */
	private static void collectPersistentAttributesUsingClassLevelAccessType(
			ClassDetails classDetails,
			AccessType classLevelAccessType,
			Map<String, MemberDetails> persistentAttributeMap,
			Map<String,MethodDetails> persistentAttributesFromGetters,
			Map<String,RecordComponentDetails> persistentAttributesFromComponents,
			List<FieldDetails> fields,
			List<MethodDetails> getters,
			List<RecordComponentDetails> recordComponents) {
		if ( classLevelAccessType == AccessType.FIELD ) {
			for ( int i = 0; i < fields.size(); i++ ) {
				final FieldDetails field = fields.get( i );
				final String name = field.getName();
				if ( persistentAttributeMap.containsKey( name ) ) {
					continue;
				}

				persistentAttributeMap.put( name, field );
			}
		}
		else {
			for ( int i = 0; i < getters.size(); i++ ) {
				final MethodDetails getterDetails = getters.get( i );
				final String name = getterDetails.resolveAttributeName();

				// HHH-10242 detect registration of the same property getter twice - eg boolean isId() + UUID getId()
				final MethodDetails previous = persistentAttributesFromGetters.get( name );
				if ( previous != null && getterDetails != previous ) {
					throw new org.hibernate.boot.MappingException(
							LOG.ambiguousPropertyMethods(
									classDetails.getName(),
									previous.getName(),
									getterDetails.getName()
							),
							new Origin( SourceType.ANNOTATION, classDetails.getName() )
					);
				}

				if ( persistentAttributeMap.containsKey( name ) ) {
					continue;
				}

				persistentAttributeMap.put( name, getterDetails );
				persistentAttributesFromGetters.put( name, getterDetails );
			}

			// When a user uses the `property` access strategy for the entity owning an embeddable,
			// we also have to add the attributes for record components,
			// because record classes usually don't have getters, but just the record component accessors
			for ( int i = 0; i < recordComponents.size(); i++ ) {
				final RecordComponentDetails componentDetails = recordComponents.get( i );
				final String name = componentDetails.getName();
				if ( persistentAttributeMap.containsKey( name ) ) {
					continue;
				}

				persistentAttributeMap.put( name, componentDetails );
				persistentAttributesFromComponents.put( name, componentDetails );
			}
		}
	}

	public ClassDetails getDeclaringClass() {
		return classDetails;
	}

	public ClassDetails getEntityAtStake() {
		return entityAtStake;
	}

	public AccessType getClassLevelAccessType() {
		return classLevelAccessType;
	}

	public Iterable<MemberDetails> propertyIterator() {
		return attributeMembers;
	}

	private static List<MemberDetails> verifyAndInitializePersistentAttributes(
			ClassDetails classDetails,
			Map<String, MemberDetails> attributeMemberMap) {
		ArrayList<MemberDetails> output = new ArrayList<>( attributeMemberMap.size() );
		for ( MemberDetails attributeMemberDetails : attributeMemberMap.values() ) {
			final TypeDetails memberType = attributeMemberDetails.getType();
			if ( !memberType.isResolved()
					&& !discoverTypeWithoutReflection( classDetails, attributeMemberDetails ) ) {
				final String msg = "Property '" + StringHelper.qualify( classDetails.getName(), attributeMemberDetails.getName() ) +
						"' has an unbound type and no explicit target entity (resolve this generics usage issue" +
						" or set an explicit target attribute with '@OneToMany(target=)' or use an explicit '@Type')";
				throw new AnnotationException( msg );
			}
			output.add( attributeMemberDetails );
		}
		return CollectionHelper.toSmallList( output );
	}

	private AccessType determineLocalClassDefinedAccessStrategy() {
		AccessType classDefinedAccessType = AccessType.DEFAULT;
		final AnnotationUsage<Access> access = classDetails.getAnnotationUsage( Access.class );
		if ( access != null ) {
			classDefinedAccessType = AccessType.getAccessStrategy( access.getEnum( "value" ) );
		}
		return classDefinedAccessType;
	}

	private static boolean discoverTypeWithoutReflection(ClassDetails classDetails, MemberDetails memberDetails) {
		if ( memberDetails.hasAnnotationUsage( Target.class ) ) {
			return true;
		}

		if ( memberDetails.hasAnnotationUsage( Basic.class ) ) {
			return true;
		}

		if ( memberDetails.hasAnnotationUsage( Type.class ) ) {
			return true;
		}

		if ( memberDetails.hasAnnotationUsage( JavaType.class ) ) {
			return true;
		}

		final AnnotationUsage<OneToOne> oneToOneAnn = memberDetails.getAnnotationUsage( OneToOne.class );
		if ( oneToOneAnn != null ) {
			final ClassDetails targetEntity = oneToOneAnn.getClassDetails( "targetEntity" );
			return targetEntity != ClassDetails.VOID_CLASS_DETAILS;
		}


		final AnnotationUsage<OneToMany> oneToManyAnn = memberDetails.getAnnotationUsage( OneToMany.class );
		if ( oneToManyAnn != null ) {
			final ClassDetails targetEntity = oneToManyAnn.getClassDetails( "targetEntity" );
			return targetEntity != ClassDetails.VOID_CLASS_DETAILS;
		}


		final AnnotationUsage<ManyToOne> manToOneAnn = memberDetails.getAnnotationUsage( ManyToOne.class );
		if ( manToOneAnn != null ) {
			final ClassDetails targetEntity = manToOneAnn.getClassDetails( "targetEntity" );
			return targetEntity != ClassDetails.VOID_CLASS_DETAILS;
		}

		final AnnotationUsage<ManyToMany> manToManyAnn = memberDetails.getAnnotationUsage( ManyToMany.class );
		if ( manToManyAnn != null ) {
			final ClassDetails targetEntity = manToManyAnn.getClassDetails( "targetEntity" );
			return targetEntity != ClassDetails.VOID_CLASS_DETAILS;
		}

		if ( memberDetails.hasAnnotationUsage( Any.class ) ) {
			return true;
		}

		final AnnotationUsage<ManyToAny> manToAnyAnn = memberDetails.getAnnotationUsage( ManyToAny.class );
		if ( manToAnyAnn != null ) {
			return true;
		}
		else if ( memberDetails.hasAnnotationUsage( JdbcTypeCode.class ) ) {
			return true;
		}

		if ( memberDetails.getType().determineRawClass().isImplementor( Class.class ) ) {
			// specialized case for @Basic attributes of type Class (or Class<?>, etc.).
			// we only really care about the Class part
			return true;
		}

		return false;
	}

	private static boolean mustBeSkipped(MemberDetails memberDetails) {
		//TODO make those hardcoded tests more portable (through the bytecode provider?)
		return memberDetails.hasAnnotationUsage( Transient.class )
				|| (memberDetails.getType() != null && "net.sf.cglib.transform.impl.InterceptFieldCallback".equals( memberDetails.getType().getName() ) );
	}
}
