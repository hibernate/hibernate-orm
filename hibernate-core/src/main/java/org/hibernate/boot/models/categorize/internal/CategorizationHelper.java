/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.EnumSet;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.AnyKeyJdbcType;
import org.hibernate.annotations.AnyKeyJdbcTypeCode;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.EmbeddableInstantiator;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.Type;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.MultipleAttributeNaturesException;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.ClassAttributeAccessType;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.Version;

import static org.hibernate.boot.models.categorize.ModelCategorizationLogging.MODEL_CATEGORIZATION_LOGGER;
import static org.hibernate.boot.models.categorize.spi.AttributeMetadata.AttributeNature.BASIC;
import static org.hibernate.boot.models.categorize.spi.AttributeMetadata.AttributeNature.ELEMENT_COLLECTION;
import static org.hibernate.boot.models.categorize.spi.AttributeMetadata.AttributeNature.EMBEDDED;
import static org.hibernate.boot.models.categorize.spi.AttributeMetadata.AttributeNature.MANY_TO_ANY;
import static org.hibernate.boot.models.categorize.spi.AttributeMetadata.AttributeNature.MANY_TO_MANY;
import static org.hibernate.boot.models.categorize.spi.AttributeMetadata.AttributeNature.ONE_TO_MANY;

/**
 * @author Steve Ebersole
 */
public class CategorizationHelper {
	public static ClassDetails toClassDetails(Class<?> clazz, ClassDetailsRegistry classDetailsRegistry) {
		return classDetailsRegistry.resolveClassDetails( clazz.getName() );
	}

	public static boolean isMappedSuperclass(ClassDetails classDetails) {
		return classDetails.hasDirectAnnotationUsage( MappedSuperclass.class );
	}

	public static boolean isEntity(ClassDetails classDetails) {
		return classDetails.getDirectAnnotationUsage( Entity.class ) != null;
	}

	public static boolean isIdentifiable(ClassDetails classDetails) {
		return isEntity( classDetails ) || isMappedSuperclass( classDetails );
	}

	public static ClassAttributeAccessType determineAccessType(ClassDetails classDetails, AccessType implicitAccessType) {
		final Access annotation = classDetails.getDirectAnnotationUsage( Access.class );
		if ( annotation != null ) {
			final AccessType explicitValue = annotation.value();
			assert explicitValue != null;
			return explicitValue == AccessType.FIELD
					? ClassAttributeAccessType.EXPLICIT_FIELD
					: ClassAttributeAccessType.EXPLICIT_PROPERTY;
		}

		return implicitAccessType == AccessType.FIELD
				? ClassAttributeAccessType.IMPLICIT_FIELD
				: ClassAttributeAccessType.IMPLICIT_PROPERTY;
	}

	/**
	 * Determine the attribute's nature - is it a basic mapping, an embeddable, ...?
	 * </p>
	 * Also performs some simple validation around multiple natures being indicated
	 */
	public static AttributeMetadata.AttributeNature determineAttributeNature(ClassDetails declarer, MemberDetails backingMember) {
		final EnumSet<AttributeMetadata.AttributeNature> natures = EnumSet.noneOf( AttributeMetadata.AttributeNature.class );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// first, look for explicit nature annotations

		final Any any = backingMember.getDirectAnnotationUsage( Any.class );
		final Basic basic = backingMember.getDirectAnnotationUsage( Basic.class );
		final ElementCollection elementCollection = backingMember.getDirectAnnotationUsage( ElementCollection.class );
		final Embedded embedded = backingMember.getDirectAnnotationUsage( Embedded.class );
		final EmbeddedId embeddedId = backingMember.getDirectAnnotationUsage( EmbeddedId.class );
		final ManyToAny manyToAny = backingMember.getDirectAnnotationUsage( ManyToAny.class );
		final ManyToMany manyToMany = backingMember.getDirectAnnotationUsage( ManyToMany.class );
		final ManyToOne manyToOne = backingMember.getDirectAnnotationUsage( ManyToOne.class );
		final OneToMany oneToMany = backingMember.getDirectAnnotationUsage( OneToMany.class );
		final OneToOne oneToOne = backingMember.getDirectAnnotationUsage( OneToOne.class );

		if ( basic != null ) {
			natures.add( AttributeMetadata.AttributeNature.BASIC );
		}

		if ( embedded != null
				|| embeddedId != null
				|| ( backingMember.getType() != null && backingMember.getType().determineRawClass().hasDirectAnnotationUsage( Embeddable.class ) ) ) {
			natures.add( EMBEDDED );
		}

		if ( any != null ) {
			natures.add( AttributeMetadata.AttributeNature.ANY );
		}

		if ( oneToOne != null || manyToOne != null ) {
			natures.add( AttributeMetadata.AttributeNature.TO_ONE );
		}

		if ( elementCollection != null ) {
			natures.add( ELEMENT_COLLECTION );
		}

		if ( oneToMany != null ) {
			natures.add( ONE_TO_MANY );
		}

		if ( manyToMany != null ) {
			natures.add( MANY_TO_MANY );
		}

		if ( manyToAny != null ) {
			natures.add( MANY_TO_ANY );
		}

		// look at annotations that imply a nature

		final boolean plural = oneToMany != null
				|| manyToMany != null
				|| elementCollection != null
				|| manyToAny != null;

		final boolean implicitlyBasic = backingMember.hasDirectAnnotationUsage( Temporal.class )
				|| backingMember.hasDirectAnnotationUsage( Lob.class )
				|| backingMember.hasDirectAnnotationUsage( Enumerated.class )
				|| backingMember.hasDirectAnnotationUsage( Version.class )
				|| backingMember.hasDirectAnnotationUsage( HibernateAnnotations.GENERATED.getAnnotationType() )
				|| backingMember.hasDirectAnnotationUsage( Nationalized.class )
				|| backingMember.hasDirectAnnotationUsage( TimeZoneColumn.class )
				|| backingMember.hasDirectAnnotationUsage( TimeZoneStorage.class )
				|| backingMember.hasDirectAnnotationUsage( Type.class )
				|| backingMember.hasDirectAnnotationUsage( TenantId.class )
				|| backingMember.hasDirectAnnotationUsage( JavaType.class )
				|| backingMember.hasDirectAnnotationUsage( JdbcType.class )
				|| backingMember.hasDirectAnnotationUsage( JdbcTypeCode.class );

		final boolean implicitlyEmbedded = backingMember.hasDirectAnnotationUsage( EmbeddableInstantiator.class )
				|| backingMember.hasDirectAnnotationUsage( CompositeType.class );

		final boolean implicitlyAny = backingMember.hasDirectAnnotationUsage( AnyDiscriminator.class )
//				|| CollectionHelper.isNotEmpty( backingMember.getRepeatedAnnotationUsages( HibernateAnnotations.ANY_DISCRIMINATOR_VALUE ) )
//				|| backingMember.hasDirectAnnotationUsage( HibernateAnnotations.ANY_DISCRIMINATOR_VALUES )
				|| backingMember.hasDirectAnnotationUsage( AnyKeyJavaType.class )
				|| backingMember.hasDirectAnnotationUsage( AnyKeyJavaClass.class )
				|| backingMember.hasDirectAnnotationUsage( AnyKeyJdbcType.class )
				|| backingMember.hasDirectAnnotationUsage( AnyKeyJdbcTypeCode.class );

		if ( !plural ) {
			// first implicit basic nature
			if ( implicitlyBasic ) {
				natures.add( AttributeMetadata.AttributeNature.BASIC );
			}

			// then embedded
			if ( implicitlyEmbedded ) {
				natures.add( EMBEDDED );
			}

			// and any
			if ( implicitlyAny ) {
				natures.add( AttributeMetadata.AttributeNature.ANY );
			}
		}
		else {
			if ( elementCollection != null ) {
				// for @ElementCollection, allow `@Basic` or `@Embedded` (though not both)
				if ( natures.contains( BASIC ) ) {
					if ( natures.contains( EMBEDDED ) ) {
						// don't do anything, this is still an error
					}
					else {
						MODEL_CATEGORIZATION_LOGGER.debugf( "Ignoring @Basic on @ElementCollection - %s", backingMember.resolveAttributeName() );
						natures.remove( BASIC );
					}
				}
				else if ( natures.contains( EMBEDDED ) ) {
					MODEL_CATEGORIZATION_LOGGER.debugf( "Ignoring @Embedded on @ElementCollection - %s", backingMember.resolveAttributeName() );
					natures.remove( EMBEDDED );
				}
			}
		}

		int size = natures.size();
		return switch ( size ) {
			case 0 -> {
				MODEL_CATEGORIZATION_LOGGER.debugf(
						"Implicitly interpreting attribute `%s` as BASIC",
						backingMember.resolveAttributeName()
				);
				yield AttributeMetadata.AttributeNature.BASIC;
			}
			case 1 -> natures.iterator().next();
			default -> throw new MultipleAttributeNaturesException(
					declarer.getName() + "#" + backingMember.resolveAttributeName(),
					natures
			);
		};
	}
}
