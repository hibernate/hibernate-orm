/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
import org.hibernate.annotations.TargetEmbeddable;
import org.hibernate.annotations.Type;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.RecordComponentDetails;
import org.hibernate.models.spi.TypeVariableScope;


import jakarta.persistence.Access;
import jakarta.persistence.Basic;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.collections.CollectionHelper.toSmallList;

/**
 * Access to the members of a {@linkplain ClassDetails class} which define a persistent attribute
 * as defined by the JPA specification and AccessType.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class PropertyContainer {

	/**
	 * The class for which this container is created.
	 */
	private final ClassDetails classDetails;
	private final TypeVariableScope typeAtStake;

	/**
	 * Holds the AccessType indicated for use at the class/container-level for cases where persistent attribute
	 * did not specify.
	 */
	private final AccessType classLevelAccessType;

	private final List<MemberDetails> attributeMembers;

	public PropertyContainer(ClassDetails classDetails, TypeVariableScope typeAtStake, AccessType defaultClassLevelAccessType) {
		this.classDetails = classDetails;
		this.typeAtStake = typeAtStake;

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

		attributeMembers = resolveAttributeMembers( classDetails, typeAtStake, classLevelAccessType );
	}

	private static List<MemberDetails> resolveAttributeMembers(
			ClassDetails classDetails,
			TypeVariableScope typeAtStake,
			AccessType classLevelAccessType) {
		final var fields = collectPotentialAttributeMembers( classDetails.getFields() );
		final var getters = collectPotentialAttributeMembers( classDetails.getMethods() );
		final var recordComponents = collectPotentialAttributeMembers( classDetails.getRecordComponents() );

		final Map<String, MemberDetails> attributeMemberMap =
				buildAttributeMemberMap( recordComponents, fields, getters );

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
		return verifyAndInitializePersistentAttributes( classDetails, typeAtStake, attributeMemberMap );
	}

	private static Map<String, MemberDetails> buildAttributeMemberMap(
			List<RecordComponentDetails> recordComponents,
			List<FieldDetails> fields,
			List<MethodDetails> getters) {
		final Map<String, MemberDetails> attributeMemberMap;
		// If the record class has only record components which match up with fields and no additional getters,
		// we must retain the property order, to match up with the record component order
		if ( !recordComponents.isEmpty() && recordComponents.size() == fields.size() ) {
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
			final var fieldDetails = fields.get( i );
			final var localAccessAnnotation = fieldDetails.getDirectAnnotationUsage( Access.class );
			if ( localAccessAnnotation != null
					&& localAccessAnnotation.value() == jakarta.persistence.AccessType.FIELD ) {
				persistentAttributeMap.put( fieldDetails.getName(), fieldDetails );
			}
		}

		// Check getters...
		for ( int i = 0; i < getters.size(); i++ ) {
			final var getterDetails = getters.get( i );
			final var localAccessAnnotation = getterDetails.getDirectAnnotationUsage( Access.class );
			if ( localAccessAnnotation != null
					&& localAccessAnnotation.value() == jakarta.persistence.AccessType.PROPERTY ) {
				final String name = getterDetails.resolveAttributeName();
				// HHH-10242 detect registration of the same property getter twice - eg boolean isId() + UUID getId()
				final var previous = persistentAttributesFromGetters.get( name );
				if ( previous != null ) {
					throwAmbiguousPropertyException( classDetails, previous, getterDetails );
				}
				persistentAttributeMap.put( name, getterDetails );
				persistentAttributesFromGetters.put( name, getterDetails );
			}

		}

		// Check record components...
		for ( int i = 0; i < recordComponents.size(); i++ ) {
			final var recordComponentDetails = recordComponents.get( i );
			if ( recordComponentDetails.hasDirectAnnotationUsage( Access.class ) ) {
				final String name = recordComponentDetails.getName();
				persistentAttributeMap.put( name, recordComponentDetails );
				persistentAttributesFromComponents.put( name, recordComponentDetails );
			}
		}
	}

	private static void throwAmbiguousPropertyException(
			ClassDetails classDetails, MethodDetails previous, MethodDetails getterDetails) {
		throw new MappingException(
				String.format(
						"Ambiguous persistent property methods declared by '%s': '%s' and '%s' (mark one '@Transient')",
						classDetails.getName(),
						previous.getName(),
						getterDetails.getName()
				),
				new Origin( SourceType.ANNOTATION, classDetails.getName() )
		);
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
					throwAmbiguousPropertyException( classDetails, previous, getterDetails );
				}

				if ( !persistentAttributeMap.containsKey( name ) ) {
					persistentAttributeMap.put( name, getterDetails );
					persistentAttributesFromGetters.put( name, getterDetails );
				}
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

	public TypeVariableScope getTypeAtStake() {
		return typeAtStake;
	}

	public AccessType getClassLevelAccessType() {
		return classLevelAccessType;
	}

	public Iterable<MemberDetails> propertyIterator() {
		return attributeMembers;
	}

	private static List<MemberDetails> verifyAndInitializePersistentAttributes(
			ClassDetails classDetails,
			TypeVariableScope typeAtStake,
			Map<String, MemberDetails> attributeMemberMap) {
		final ArrayList<MemberDetails> output = new ArrayList<>( attributeMemberMap.size() );
		for ( var attributeMemberDetails : attributeMemberMap.values() ) {
			if ( !attributeMemberDetails.resolveRelativeType( typeAtStake ).isResolved()
					&& !discoverTypeWithoutReflection( attributeMemberDetails ) ) {
				final String msg = "Property '" + qualify( classDetails.getName(), attributeMemberDetails.getName() ) +
						"' has an unbound type and no explicit target entity (resolve this generics usage issue" +
						" or set an explicit target attribute with '@OneToMany(target=)' or use an explicit '@Type')";
				throw new AnnotationException( msg );
			}
			output.add( attributeMemberDetails );
		}
		return toSmallList( output );
	}

	private AccessType determineLocalClassDefinedAccessStrategy() {
		final var access = classDetails.getDirectAnnotationUsage( Access.class );
		return access == null ? AccessType.DEFAULT : AccessType.getAccessStrategy( access.value() );
	}

	private static boolean discoverTypeWithoutReflection(MemberDetails memberDetails) {
		if ( memberDetails.hasDirectAnnotationUsage( TargetEmbeddable.class ) ) {
			return true;
		}

		if ( memberDetails.hasDirectAnnotationUsage( Basic.class ) ) {
			return true;
		}

		if ( memberDetails.hasDirectAnnotationUsage( Type.class ) ) {
			return true;
		}

		if ( memberDetails.hasDirectAnnotationUsage( JavaType.class ) ) {
			return true;
		}

		final var oneToOneAnn = memberDetails.getDirectAnnotationUsage( OneToOne.class );
		if ( oneToOneAnn != null ) {
			return oneToOneAnn.targetEntity() != void.class;
		}

		final var oneToManyAnn = memberDetails.getDirectAnnotationUsage( OneToMany.class );
		if ( oneToManyAnn != null ) {
			return oneToManyAnn.targetEntity() != void.class;
		}

		final var manyToOneAnn = memberDetails.getDirectAnnotationUsage( ManyToOne.class );
		if ( manyToOneAnn != null ) {
			return manyToOneAnn.targetEntity() != void.class;
		}

		final var manyToManyAnn = memberDetails.getDirectAnnotationUsage( ManyToMany.class );
		if ( manyToManyAnn != null ) {
			return manyToManyAnn.targetEntity() != void.class;
		}

		if ( memberDetails.hasDirectAnnotationUsage( Any.class ) ) {
			return true;
		}

		final var manToAnyAnn = memberDetails.getDirectAnnotationUsage( ManyToAny.class );
		if ( manToAnyAnn != null ) {
			return true;
		}

		if ( memberDetails.hasDirectAnnotationUsage( JdbcTypeCode.class ) ) {
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
		return memberDetails.hasDirectAnnotationUsage( Transient.class )
			|| memberDetails.getType() != null
				&& "net.sf.cglib.transform.impl.InterceptFieldCallback".equals( memberDetails.getType().getName() );
	}
}
