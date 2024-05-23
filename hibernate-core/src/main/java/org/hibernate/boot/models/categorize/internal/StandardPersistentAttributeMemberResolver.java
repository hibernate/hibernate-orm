/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.AnyKeyJdbcType;
import org.hibernate.annotations.AnyKeyJdbcTypeCode;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.Type;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.models.AccessTypePlacementException;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.categorize.spi.ClassAttributeAccessType;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.annotation.Generated;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.Version;

/**
 * Standard implementation of the PersistentAttributeMemberResolver contract
 * based strictly on the JPA specification.
 *
 * @author Steve Ebersole
 */
public class StandardPersistentAttributeMemberResolver extends AbstractPersistentAttributeMemberResolver {
	/**
	 * Singleton access
	 */
	public static final StandardPersistentAttributeMemberResolver INSTANCE = new StandardPersistentAttributeMemberResolver();

	@Override
	protected List<MemberDetails> resolveAttributesMembers(
			Function<FieldDetails,Boolean> transientFieldChecker,
			Function<MethodDetails,Boolean> transientMethodChecker,
			ClassDetails classDetails,
			ClassAttributeAccessType classLevelAccessType) {
		final LinkedHashMap<String,MemberDetails> results = new LinkedHashMap<>();

		processAttributeLevelAccess(
				results::put,
				transientFieldChecker,
				transientMethodChecker,
				classDetails,
				classLevelAccessType
		);

		processClassLevelAccess(
				results::containsKey,
				results::put,
				transientFieldChecker,
				transientMethodChecker,
				classDetails,
				classLevelAccessType
		);

		return new ArrayList<>( results.values() );
	}

	private <M extends MemberDetails> void processAttributeLevelAccess(
			BiConsumer<String,MemberDetails> memberConsumer,
			Function<FieldDetails,Boolean> transientFieldChecker,
			Function<MethodDetails,Boolean> transientMethodChecker,
			ClassDetails classDetails,
			ClassAttributeAccessType classLevelAccessType) {
		final List<FieldDetails> fields = classDetails.getFields();
		for ( int i = 0; i < fields.size(); i++ ) {
			final FieldDetails fieldDetails = fields.get( i );
			processAttributeLevelAccessMember( fieldDetails, memberConsumer, transientFieldChecker, classDetails, classLevelAccessType );
		}

		final List<MethodDetails> methods = classDetails.getMethods();
		for ( int i = 0; i < methods.size(); i++ ) {
			final MethodDetails methodDetails = methods.get( i );
			processAttributeLevelAccessMember( methodDetails, memberConsumer, transientMethodChecker, classDetails, classLevelAccessType );
		}
	}

	private <M extends MemberDetails> void processAttributeLevelAccessMember(
			M memberDetails,
			BiConsumer<String,MemberDetails> memberConsumer,
			Function<M,Boolean> transiencyChecker,
			ClassDetails classDetails,
			ClassAttributeAccessType classLevelAccessType) {
		if ( transiencyChecker.apply( memberDetails ) ) {
			// the field is transient
			return;
		}

		final Access access = memberDetails.getDirectAnnotationUsage( Access.class );
		if ( access == null  ) {
			checkForMisplacedAnnotations( classDetails, memberDetails, classLevelAccessType );
			return;
		}

		final AccessType attributeAccessType = access.value();
		validateAttributeLevelAccess( memberDetails, attributeAccessType, classDetails );

		memberConsumer.accept( memberDetails.resolveAttributeName(), memberDetails );
	}

	private <M extends MemberDetails> void checkForMisplacedAnnotations(
			ClassDetails classDetails,
			M memberDetails,
			ClassAttributeAccessType classLevelAccessType) {
		// We have a case where the member did not define `@Access`.
		//
		// In such a case the member would only be an attribute backer if the member
		// kind matched the class access-type.  If the member kind does *not* match
		// the class access-type, validate that it does not declare any mapping annotations

		if ( classLevelAccessType == null ) {
			// nothing to check
			return;
		}

		if ( !matchesAccessType( memberDetails, classLevelAccessType ) ) {
			if ( containsMappingAnnotations( memberDetails ) ) {
				if ( memberDetails.getKind() == AnnotationTarget.Kind.FIELD ) {
					throw new AnnotationPlacementException(
							String.format(
									Locale.ROOT,
									"Field `%s#%s` declared mapping annotations even though it is not a persistent attribute",
									classDetails.getName(),
									memberDetails.getName()
							)
					);
				}
				else {
					assert memberDetails.getKind() == AnnotationTarget.Kind.METHOD;
					throw new AnnotationPlacementException(
							String.format(
									Locale.ROOT,
									"Method `%s#%s` declared mapping annotations even though it is not a persistent attribute",
									classDetails.getName(),
									memberDetails.getName()
							)
					);
				}
			}
		}
	}

	private <M extends MemberDetails> boolean matchesAccessType(
			M memberDetails,
			ClassAttributeAccessType classLevelAccessType) {
		assert classLevelAccessType != null;
		if ( classLevelAccessType.getJpaAccessType() == AccessType.FIELD ) {
			return memberDetails.getKind() == AnnotationTarget.Kind.FIELD;
		}
		else {
			return memberDetails.getKind() == AnnotationTarget.Kind.METHOD
					&& ( (MethodDetails) memberDetails ).getMethodKind() == MethodDetails.MethodKind.GETTER;
		}
	}

	private <M extends MemberDetails> boolean containsMappingAnnotations(M memberDetails) {
		// todo (jpa32) : better way to do this?
		return memberDetails.hasDirectAnnotationUsage( Id.class )
				|| memberDetails.hasDirectAnnotationUsage( EmbeddedId.class )
				|| memberDetails.hasDirectAnnotationUsage( Version.class )
				|| memberDetails.hasDirectAnnotationUsage( Basic.class )
				|| memberDetails.hasDirectAnnotationUsage( Embedded.class )
				|| memberDetails.hasDirectAnnotationUsage( ManyToOne.class )
				|| memberDetails.hasDirectAnnotationUsage( OneToOne.class )
				|| memberDetails.hasDirectAnnotationUsage( ElementCollection.class )
				|| memberDetails.hasDirectAnnotationUsage( ManyToMany.class )
				|| memberDetails.hasDirectAnnotationUsage( OneToMany.class )
				|| memberDetails.hasDirectAnnotationUsage( Any.class )
				|| memberDetails.hasDirectAnnotationUsage( ManyToAny.class )
				|| memberDetails.hasDirectAnnotationUsage( AnyKeyJavaClass.class )
				|| memberDetails.hasDirectAnnotationUsage( AnyKeyJavaType.class )
				|| memberDetails.hasDirectAnnotationUsage( AnyKeyJdbcType.class )
				|| memberDetails.hasDirectAnnotationUsage( AnyKeyJdbcTypeCode.class )
				|| memberDetails.hasDirectAnnotationUsage( AnyKeyType.class )
				|| memberDetails.hasDirectAnnotationUsage( AnyDiscriminator.class )
				|| memberDetails.hasDirectAnnotationUsage( AnyDiscriminatorValue.class )
				|| memberDetails.hasDirectAnnotationUsage( AnyDiscriminatorValues.class )
				|| memberDetails.hasDirectAnnotationUsage( Column.class )
				|| memberDetails.hasDirectAnnotationUsage( Enumerated.class )
				|| memberDetails.hasDirectAnnotationUsage( Lob.class )
				|| memberDetails.hasDirectAnnotationUsage( Temporal.class )
				|| memberDetails.hasDirectAnnotationUsage( Nationalized.class )
				|| memberDetails.hasDirectAnnotationUsage( TenantId.class )
				|| memberDetails.hasDirectAnnotationUsage( Generated.class )
				|| memberDetails.hasDirectAnnotationUsage( TimeZoneColumn.class )
				|| memberDetails.hasDirectAnnotationUsage( TimeZoneStorage.class )
				|| memberDetails.hasDirectAnnotationUsage( Type.class )
				|| memberDetails.hasDirectAnnotationUsage( JavaType.class )
				|| memberDetails.hasDirectAnnotationUsage( JdbcType.class )
				|| memberDetails.hasDirectAnnotationUsage( JdbcTypeCode.class );
	}

	private void validateAttributeLevelAccess(
			MemberDetails annotationTarget,
			AccessType attributeAccessType,
			ClassDetails classDetails) {
		// Apply the checks defined in section `2.3.2 Explicit Access Type` of the persistence specification

		// Mainly, it is never legal to:
		//		1. specify @Access(FIELD) on a getter
		//		2. specify @Access(PROPERTY) on a field

		// todo (jpa32) : pass along access to JpaCompliance and use a new `JpaCompliance#isAnnotationPlacementComplianceEnabled` method here
		// 		- for now, just allow it as we interpret the actual attribute AccessType value to dictate the state access
		if ( !isAnnotationPlacementComplianceEnabled() ) {
			return;
		}

		if ( ( attributeAccessType == AccessType.FIELD && !annotationTarget.isField() )
				|| ( attributeAccessType == AccessType.PROPERTY && annotationTarget.isField() ) ) {
			throw new AccessTypePlacementException( classDetails, annotationTarget );
		}
	}

	private boolean isAnnotationPlacementComplianceEnabled() {
		return false;
	}

	private void processClassLevelAccess(
			Function<String,Boolean> alreadyProcessedChecker,
			BiConsumer<String, MemberDetails> memberConsumer,
			Function<FieldDetails,Boolean> transientFieldChecker,
			Function<MethodDetails,Boolean> transientMethodChecker,
			ClassDetails classDetails,
			ClassAttributeAccessType classLevelAccessType) {
		if ( classLevelAccessType == null ) {
			return;
		}

		if ( classLevelAccessType.getJpaAccessType() == AccessType.FIELD ) {
			final List<FieldDetails> fields = classDetails.getFields();
			for ( int i = 0; i < fields.size(); i++ ) {
				final FieldDetails fieldDetails = fields.get( i );
				if ( !fieldDetails.isPersistable() ) {
					// the field cannot be a persistent attribute
					continue;
				}

				final String attributeName = fieldDetails.resolveAttributeName();
				if ( alreadyProcessedChecker.apply( attributeName ) ) {
					continue;
				}

				if ( transientFieldChecker.apply( fieldDetails ) ) {
					// the field is @Transient
					continue;
				}

				memberConsumer.accept( attributeName, fieldDetails );
			}
		}
		else {
			assert classLevelAccessType.getJpaAccessType() == AccessType.PROPERTY;
			final List<MethodDetails> methods = classDetails.getMethods();
			for ( int i = 0; i < methods.size(); i++ ) {
				final MethodDetails methodDetails = methods.get( i );
				if ( !methodDetails.isPersistable() ) {
					continue;
				}

				final String attributeName = methodDetails.resolveAttributeName();
				if ( alreadyProcessedChecker.apply( attributeName ) ) {
					continue;
				}

				if ( transientMethodChecker.apply( methodDetails ) ) {
					// the method is @Transient
					continue;
				}

				memberConsumer.accept( attributeName, methodDetails );
			}
		}
	}
}
