/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.FetchStyle;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.attribute.PersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * A helper with utilities for working with associations
 *
 * @author Steve Ebersole
 */
public class AssociationHelper {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AssociationHelper.class );

	/**
	 * Singleton access
	 */
	public static final AssociationHelper INSTANCE = new AssociationHelper();

	private AssociationHelper() {
	}


	/**
	 * Determine the concrete "target type" name
	 *
	 * @param backingMember The attribute member
	 * @param associationAnnotation The annotation that identifies the "association"
	 * @param defaultTargetType The default type for the target.  For singular attributes, this is the
	 * attribute type; for plural attributes it is the element type.
	 * @param context The binding context
	 *
	 * @return The concrete type name
	 */
	public String determineTarget(
			MemberDescriptor backingMember,
			AnnotationInstance associationAnnotation,
			JavaTypeDescriptor defaultTargetType,
			EntityBindingContext context) {
		final AnnotationInstance targetAnnotation = backingMember.getAnnotations().get( HibernateDotNames.TARGET );
		if ( targetAnnotation != null ) {
			return targetAnnotation.value().asString();
		}

		final AnnotationValue targetEntityValue = associationAnnotation.value( "targetEntity" );
		if ( targetEntityValue != null ) {
			return targetEntityValue.asString();
		}

		if ( defaultTargetType == null ) {
			throw context.makeMappingException(
					"Could not determine target for association : " + backingMember.toString()
			);
		}

		return defaultTargetType.getName().toString();
	}

	/**
	 * Determine the name of the attribute that is the other side of this association, which
	 * contains mapping metadata.
	 *
	 * @param associationAnnotation The annotation that identifies the "association"
	 *
	 * @return The specified mapped-by attribute name, or {@code null} if none specified
	 */
	public String determineMappedByAttributeName(AnnotationInstance associationAnnotation) {
		if ( associationAnnotation == null ) {
			return null;
		}

		final AnnotationValue mappedByAnnotationValue = associationAnnotation.value( "mappedBy" );
		if ( mappedByAnnotationValue == null ) {
			return null;
		}

		return mappedByAnnotationValue.asString();
	}

	/**
	 * Given a member that is fetchable, determine the indicated FetchStyle
	 *
	 * @param backingMember The fetchable attribute member
	 *
	 * @return The indicated FetchStyle, or {@code null} if no indication was given
	 */
	public FetchStyle determineFetchStyle(MemberDescriptor backingMember) {
		final AnnotationInstance fetchAnnotation = backingMember.getAnnotations().get( HibernateDotNames.FETCH );
		if ( fetchAnnotation == null ) {
			return null;
		}

		final org.hibernate.annotations.FetchMode annotationFetchMode = org.hibernate.annotations.FetchMode.valueOf(
				fetchAnnotation.value().asEnum()
		);
		return EnumConversionHelper.annotationFetchModeToFetchStyle( annotationFetchMode );
	}

	/**
	 * Determine whether the given (fetchable?) association is considered lazy.
	 *
	 * @param associationAnnotation The annotation that identifies the "association"
	 * @param lazyAnnotation The Hibernate-specific lazy annotation for attributes of the given nature.  Generally
	 * {@link org.hibernate.annotations.LazyCollection} or {@link org.hibernate.annotations.LazyToOne}
	 * @param backingMember The fetchable attribute member
	 * @param fetchStyle The specified fetch style
	 *
	 * @return whether its considered lazy, duh :)
	 */
	public boolean determineWhetherIsLazy(
			AnnotationInstance associationAnnotation,
			AnnotationInstance lazyAnnotation,
			MemberDescriptor backingMember,
			FetchStyle fetchStyle,
			boolean isCollection) {

		// first precedence
		// 		- join fetches cannot be lazy : fetch style
		if ( fetchStyle != null ) {
			if ( fetchStyle == FetchStyle.JOIN ) {
				return false;
			}
		}

		// second precedence
		// 		- join fetches cannot be lazy : fetch annotation
		final AnnotationInstance fetchAnnotation = backingMember.getAnnotations().get(
				HibernateDotNames.FETCH
		);
		if ( fetchAnnotation != null ) {
			if ( FetchMode.valueOf( fetchAnnotation.value().asEnum() ) == FetchMode.JOIN ) {
				return false;
			}
		}

		// 3rd precedence
		final AnnotationValue fetchValue = associationAnnotation.value( "fetch" );
		if ( fetchValue != null ) {
			return FetchType.LAZY == FetchType.valueOf( fetchValue.asEnum() );
		}

		// 4th precedence
		if ( lazyAnnotation != null ) {
			final AnnotationValue value = lazyAnnotation.value();
			if ( value != null ) {
				return !"FALSE".equals( value.asEnum() );
			}
		}

		// by default collections are lazy, to-ones are not
		return isCollection;
	}


	public boolean determineOptionality(AnnotationInstance associationAnnotation) {
		// todo : is this only valid for singular association attributes?

		boolean optional = true;

		AnnotationValue optionalValue = associationAnnotation.value( "optional" );
		if ( optionalValue != null ) {
			optional = optionalValue.asBoolean();
		}

		return optional;
	}

	public boolean determineWhetherToUnwrapProxy(MemberDescriptor backingMember) {
		final AnnotationInstance lazyToOne = backingMember.getAnnotations().get( HibernateDotNames.LAZY_TO_ONE );
		return lazyToOne != null && LazyToOneOption.valueOf( lazyToOne.value().asEnum() ) == LazyToOneOption.NO_PROXY;

	}

	public Set<CascadeType> determineCascadeTypes(AnnotationInstance associationAnnotation) {
		Set<CascadeType> cascadeTypes = new HashSet<CascadeType>();
		AnnotationValue cascadeValue = associationAnnotation.value( "cascade" );
		if ( cascadeValue != null ) {
			String[] cascades = cascadeValue.asEnumArray();
			for ( String s : cascades ) {
				cascadeTypes.add( Enum.valueOf( CascadeType.class, s ) );
			}
		}
		return cascadeTypes;
	}

	public Set<org.hibernate.annotations.CascadeType> determineHibernateCascadeTypes(MemberDescriptor backingMember) {
		final AnnotationInstance cascadeAnnotation = backingMember.getAnnotations().get( HibernateDotNames.CASCADE );

		if ( cascadeAnnotation != null ) {
			final AnnotationValue cascadeValue = cascadeAnnotation.value();
			if ( cascadeValue != null ) {
				final String[] cascades = cascadeValue.asEnumArray();
				if ( cascades != null && cascades.length > 0 ) {
					final Set<org.hibernate.annotations.CascadeType> cascadeTypes
							= new HashSet<org.hibernate.annotations.CascadeType>();
					for ( String cascade : cascades ) {
						cascadeTypes.add(
								org.hibernate.annotations.CascadeType.valueOf( cascade )
						);
					}
					return cascadeTypes;
				}
			}
		}

		return Collections.emptySet();
	}

	@SuppressWarnings("SimplifiableIfStatement")
	public boolean determineOrphanRemoval(AnnotationInstance associationAnnotation) {
		final AnnotationValue orphanRemovalValue = associationAnnotation.value( "orphanRemoval" );
		if ( orphanRemovalValue != null ) {
			return orphanRemovalValue.asBoolean();
		}
		return false;
	}

	public AnnotationInstance locateMapsId(
			MemberDescriptor member,
			PersistentAttribute.Nature attributeNature,
			EntityBindingContext localBindingContext) {
		final AnnotationInstance mapsIdAnnotation = member.getAnnotations().get( JPADotNames.MAPS_ID );
		if ( mapsIdAnnotation == null ) {
			return null;
		}

		// only valid for to-one associations
		if ( PersistentAttribute.Nature.MANY_TO_ONE != attributeNature
				&& PersistentAttribute.Nature.ONE_TO_ONE != attributeNature ) {
			throw localBindingContext.makeMappingException(
					"@MapsId can only be specified on a many-to-one or one-to-one " +
							"associations, property: " + member.toString()
			);
		}
		return mapsIdAnnotation;
	}

	public boolean determineWhetherToIgnoreNotFound(MemberDescriptor backingMember) {
		final AnnotationInstance notFoundAnnotation = backingMember.getAnnotations().get(
				HibernateDotNames.NOT_FOUND
		);
		if ( notFoundAnnotation != null ) {
			final AnnotationValue actionValue = notFoundAnnotation.value( "action" );
			if ( actionValue != null ) {
				return NotFoundAction.valueOf( actionValue.asEnum() ) == NotFoundAction.IGNORE;
			}
		}

		return false;
	}



	public void processJoinColumnAnnotations(
			MemberDescriptor backingMember,
			ArrayList<Column> joinColumnValues,
			EntityBindingContext context) {
		final ClassLoaderService classLoaderService = context.getServiceRegistry().getService( ClassLoaderService.class );

		final Collection<AnnotationInstance> joinColumnAnnotations = JandexHelper.getCombinedAnnotations(
				backingMember.getAnnotations(),
				JPADotNames.JOIN_COLUMN,
				JPADotNames.JOIN_COLUMNS,
				classLoaderService
		);
		for ( AnnotationInstance joinColumnAnnotation : joinColumnAnnotations ) {
			joinColumnValues.add( new Column( joinColumnAnnotation ) );
		}

		// @JoinColumn as part of @CollectionTable
		AnnotationInstance collectionTableAnnotation = backingMember.getAnnotations().get(
				JPADotNames.COLLECTION_TABLE
		);
		if ( collectionTableAnnotation != null ) {
			final AnnotationInstance[] joinColumnsList = JandexHelper.getValue(
					collectionTableAnnotation,
					"joinColumns",
					AnnotationInstance[].class,
					classLoaderService
			);
			for ( AnnotationInstance annotation : joinColumnsList ) {
				joinColumnValues.add( new Column( annotation ) );
			}
		}
	}

	public void processJoinTableAnnotations(
			MemberDescriptor backingMember,
			ArrayList<Column> joinColumnValues,
			ArrayList<Column> inverseJoinColumnValues,
			EntityBindingContext context) {
		// @JoinColumn as part of @JoinTable
		final AnnotationInstance joinTableAnnotation = backingMember.getAnnotations().get(
				JPADotNames.JOIN_TABLE
		);

		if ( joinTableAnnotation == null ) {
			return;
		}

		// first process the `joinColumns` attribute
		final AnnotationInstance[] joinColumns = JandexHelper.getValue(
				joinTableAnnotation,
				"joinColumns",
				AnnotationInstance[].class,
				context.getServiceRegistry().getService( ClassLoaderService.class )
		);
		for ( AnnotationInstance annotation : joinColumns ) {
			joinColumnValues.add( new Column( annotation ) );
		}

		// then the `inverseJoinColumns` attribute
		final AnnotationInstance[] inverseJoinColumns = JandexHelper.getValue(
				joinTableAnnotation,
				"inverseJoinColumns",
				AnnotationInstance[].class,
				context.getServiceRegistry().getService( ClassLoaderService.class )
		);
		for ( AnnotationInstance annotation : inverseJoinColumns ) {
			inverseJoinColumnValues.add( new Column( annotation ) );
		}
	}

	public AnnotationInstance extractExplicitJoinTable(MemberDescriptor backingMember, EntityBindingContext context) {
		final AnnotationInstance collectionTableAnnotation = backingMember.getAnnotations().get(
				JPADotNames.COLLECTION_TABLE
		);
		final AnnotationInstance joinTableAnnotation = backingMember.getAnnotations().get(
				JPADotNames.JOIN_TABLE
		);

		if ( collectionTableAnnotation != null && joinTableAnnotation != null ) {
			throw context.makeMappingException(
					log.collectionTableAndJoinTableUsedTogether(
							context.getOrigin().getName(),
							backingMember.getName()
					)
			);
		}

		if ( collectionTableAnnotation != null ) {
			if ( backingMember.getAnnotations().get( JPADotNames.ELEMENT_COLLECTION ) == null ) {
				throw context.makeMappingException(
						log.collectionTableWithoutElementCollection(
								context.getOrigin().getName(),
								backingMember.getName()
						)
				);
			}
			return collectionTableAnnotation;
		}

		if ( joinTableAnnotation != null ) {
			if ( backingMember.getAnnotations().get( JPADotNames.ONE_TO_ONE ) == null
					&& backingMember.getAnnotations().get( JPADotNames.ONE_TO_MANY ) == null
					&& backingMember.getAnnotations().get( JPADotNames.MANY_TO_MANY ) == null
					&& backingMember.getAnnotations().get( JPADotNames.MANY_TO_ONE ) == null) {
				throw context.makeMappingException(
						log.joinTableForNonAssociationAttribute(
								context.getOrigin().getName(),
								backingMember.getName()
						)
				);
			}
			return joinTableAnnotation;
		}

		return null;
	}
}
