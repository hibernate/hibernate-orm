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
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.categorize.spi.ClassAttributeAccessType;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
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

	@Override
	protected List<MemberDetails> resolveAttributesMembers(
			Function<FieldDetails,Boolean> transientFieldChecker,
			Function<MethodDetails,Boolean> transientMethodChecker,
			ClassDetails classDetails,
			ClassAttributeAccessType classLevelAccessType) {
		final LinkedHashMap<String,MemberDetails> results = new LinkedHashMap<>();

//		if ( classLevelAccessType.isExplicit() ) {
			// within a class which has an explicit class-level @Access, individual
			// attributes may override the class-level access type by an explicit @Access
			// local to the member - process those
			processAttributeLevelAccess(
					results::put,
					transientFieldChecker,
					transientMethodChecker,
					classDetails,
					classLevelAccessType
			);
//		}

		// process members based on the class-level access
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

	/**
	 * Process members of the class looking for those with explicit @Access annotations
	 *
	 * @apiNote Only valid for classes with an explicit @Access
	 */
	private void processAttributeLevelAccess(
			BiConsumer<String,MemberDetails> memberConsumer,
			Function<FieldDetails,Boolean> transientFieldChecker,
			Function<MethodDetails,Boolean> transientMethodChecker,
			ClassDetails classDetails,
			ClassAttributeAccessType classLevelAccessType) {
		if ( classLevelAccessType.getJpaAccessType() == AccessType.FIELD ) {
			// when the class-level access is defined as FIELD we will process fields in the next phase,
			// so just need to process methods here looking for explicit @Access annotations
			final List<MethodDetails> methods = classDetails.getMethods();
			for ( int i = 0; i < methods.size(); i++ ) {
				final MethodDetails methodDetails = methods.get( i );
				processAttributeLevelAccessMember( methodDetails, memberConsumer, transientMethodChecker, classDetails, classLevelAccessType );
			}
		}
		else {
			assert classLevelAccessType.getJpaAccessType() == AccessType.PROPERTY;
			// when the class-level access is defined as PROPERTY we will process methods in the next phase,
			// so just need to process fields here looking for explicit @Access annotations
			final List<FieldDetails> fields = classDetails.getFields();
			for ( int i = 0; i < fields.size(); i++ ) {
				final FieldDetails fieldDetails = fields.get( i );
				processAttributeLevelAccessMember( fieldDetails, memberConsumer, transientFieldChecker, classDetails, classLevelAccessType );
			}
		}
	}

	/**
	 * Determine if the given member defines an attribute based on the presence of a local @Access annotation
	 */
	private <M extends MemberDetails> void processAttributeLevelAccessMember(
			M memberDetails,
			BiConsumer<String,MemberDetails> memberConsumer,
			Function<M,Boolean> transiencyChecker,
			ClassDetails classDetails,
			ClassAttributeAccessType classLevelAccessType) {
		if ( transiencyChecker.apply( memberDetails ) ) {
			// the member is transient
			return;
		}

		final AnnotationUsage<Access> access = memberDetails.getAnnotationUsage( JpaAnnotations.ACCESS );
		if ( access == null  ) {
			// there was no local @Access, so not an attribute.
			// make sure there are no other mapping annotations either.
			checkForMisplacedAnnotations( classDetails, memberDetails, classLevelAccessType );
			return;
		}
//
//		final AccessType attributeAccessType = access.getAttributeValue( "value" );
//		validateAttributeLevelAccess( memberDetails, attributeAccessType, classDetails, classLevelAccessType );

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

		if ( classLevelAccessType.getTargetKind() != memberDetails.getKind() ) {
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
		// todo (7.0) : should we leverage JpaAnnotations and HibernateAnnotations to perform a more complete check?
		return memberDetails.hasAnnotationUsage( Id.class )
				|| memberDetails.hasAnnotationUsage( EmbeddedId.class )
				|| memberDetails.hasAnnotationUsage( Version.class )
				|| memberDetails.hasAnnotationUsage( Basic.class )
				|| memberDetails.hasAnnotationUsage( Embedded.class )
				|| memberDetails.hasAnnotationUsage( ManyToOne.class )
				|| memberDetails.hasAnnotationUsage( OneToOne.class )
				|| memberDetails.hasAnnotationUsage( ElementCollection.class )
				|| memberDetails.hasAnnotationUsage( ManyToMany.class )
				|| memberDetails.hasAnnotationUsage( OneToMany.class )
				|| memberDetails.hasAnnotationUsage( Any.class )
				|| memberDetails.hasAnnotationUsage( ManyToAny.class )
				|| memberDetails.hasAnnotationUsage( AnyKeyJavaClass.class )
				|| memberDetails.hasAnnotationUsage( AnyKeyJavaType.class )
				|| memberDetails.hasAnnotationUsage( AnyKeyJdbcType.class )
				|| memberDetails.hasAnnotationUsage( AnyKeyJdbcTypeCode.class )
				|| memberDetails.hasAnnotationUsage( AnyKeyType.class )
				|| memberDetails.hasAnnotationUsage( AnyDiscriminator.class )
				|| memberDetails.hasAnnotationUsage( AnyDiscriminatorValue.class )
				|| memberDetails.hasAnnotationUsage( AnyDiscriminatorValues.class )
				|| memberDetails.hasAnnotationUsage( Column.class )
				|| memberDetails.hasAnnotationUsage( Enumerated.class )
				|| memberDetails.hasAnnotationUsage( Lob.class )
				|| memberDetails.hasAnnotationUsage( Temporal.class )
				|| memberDetails.hasAnnotationUsage( Nationalized.class )
				|| memberDetails.hasAnnotationUsage( TenantId.class )
				|| memberDetails.hasAnnotationUsage( Generated.class )
				|| memberDetails.hasAnnotationUsage( TimeZoneColumn.class )
				|| memberDetails.hasAnnotationUsage( TimeZoneStorage.class )
				|| memberDetails.hasAnnotationUsage( Type.class )
				|| memberDetails.hasAnnotationUsage( JavaType.class )
				|| memberDetails.hasAnnotationUsage( JdbcType.class )
				|| memberDetails.hasAnnotationUsage( JdbcTypeCode.class );
	}

	public static void validateAttributeLevelAccess(
			MemberDetails annotationTarget,
			AccessType attributeAccessType,
			ClassDetails classDetails,
			ClassAttributeAccessType classLevelAccessType) {
		// Apply the checks defined in section `2.3.2 Explicit Access Type` of the persistence specification

		// Mainly, it is never legal to:
		//		1. specify @Access(FIELD) on a getter
		//		2. specify @Access(PROPERTY) on a field

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
