/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations.attribute;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.FetchMode;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.annotations.EnumConversionHelper;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.source.annotations.attribute.type.AttributeTypeResolverImpl;
import org.hibernate.metamodel.source.annotations.attribute.type.CompositeAttributeTypeResolver;
import org.hibernate.metamodel.source.annotations.entity.EntityBindingContext;

/**
 * Represents an association attribute.
 *
 * @author Hardy Ferentschik
 * @todo Check whether we need further subclasses for different association types. Needs to evolve during development (HF)
 */
public class AssociationAttribute extends MappedAttribute {
	private final AttributeNature associationNature;
	private final boolean ignoreNotFound;
	private final String referencedEntityType;
	private final String mappedBy;
	private final Set<CascadeType> cascadeTypes;
	private final boolean isOptional;
	private final boolean isLazy;
	private final boolean isOrphanRemoval;
	private final FetchMode fetchMode;
	private final boolean mapsId;
	private final String referencedIdAttributeName;

	private boolean isInsertable = true;
	private boolean isUpdatable = true;
	private AttributeTypeResolver resolver;

	public static AssociationAttribute createAssociationAttribute(String name,
																  Class<?> attributeType,
																  AttributeNature attributeNature,
																  String accessType,
																  Map<DotName, List<AnnotationInstance>> annotations,
																  EntityBindingContext context) {
		return new AssociationAttribute(
				name,
				attributeType,
				attributeNature,
				accessType,
				annotations,
				context
		);
	}

	private AssociationAttribute(String name,
								 Class<?> javaType,
								 AttributeNature associationType,
								 String accessType,
								 Map<DotName, List<AnnotationInstance>> annotations,
								 EntityBindingContext context) {
		super( name, javaType, accessType, annotations, context );
		this.associationNature = associationType;
		this.ignoreNotFound = ignoreNotFound();

		AnnotationInstance associationAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				associationType.getAnnotationDotName()
		);

		// using jandex we don't really care which exact type of annotation we are dealing with
		this.referencedEntityType = determineReferencedEntityType( associationAnnotation );
		this.mappedBy = determineMappedByAttributeName( associationAnnotation );
		this.isOptional = determineOptionality( associationAnnotation );
		this.isLazy = determineFetchType( associationAnnotation );
		this.isOrphanRemoval = determineOrphanRemoval( associationAnnotation );
		this.cascadeTypes = determineCascadeTypes( associationAnnotation );

		this.fetchMode = determineFetchMode();
		this.referencedIdAttributeName = determineMapsId();
		this.mapsId = referencedIdAttributeName != null;
	}

	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public String getReferencedEntityType() {
		return referencedEntityType;
	}

	public String getMappedBy() {
		return mappedBy;
	}

	public AttributeNature getAssociationNature() {
		return associationNature;
	}

	public Set<CascadeType> getCascadeTypes() {
		return cascadeTypes;
	}

	public boolean isOrphanRemoval() {
		return isOrphanRemoval;
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public String getReferencedIdAttributeName() {
		return referencedIdAttributeName;
	}

	public boolean mapsId() {
		return mapsId;
	}

	@Override
	public AttributeTypeResolver getHibernateTypeResolver() {
		if ( resolver == null ) {
			resolver = getDefaultHibernateTypeResolver();
		}
		return resolver;
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	@Override
	public boolean isOptional() {
		return isOptional;
	}

	@Override
	public boolean isInsertable() {
		return isInsertable;
	}

	@Override
	public boolean isUpdatable() {
		return isUpdatable;
	}

	@Override
	public PropertyGeneration getPropertyGeneration() {
		return PropertyGeneration.NEVER;
	}

	private AttributeTypeResolver getDefaultHibernateTypeResolver() {
		return new CompositeAttributeTypeResolver( new AttributeTypeResolverImpl( this ) );
	}

	private boolean ignoreNotFound() {
		NotFoundAction action = NotFoundAction.EXCEPTION;
		AnnotationInstance notFoundAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.NOT_FOUND
		);
		if ( notFoundAnnotation != null ) {
			AnnotationValue actionValue = notFoundAnnotation.value( "action" );
			if ( actionValue != null ) {
				action = Enum.valueOf( NotFoundAction.class, actionValue.asEnum() );
			}
		}

		return NotFoundAction.IGNORE.equals( action );
	}

	private boolean determineOptionality(AnnotationInstance associationAnnotation) {
		boolean optional = true;

		AnnotationValue optionalValue = associationAnnotation.value( "optional" );
		if ( optionalValue != null ) {
			optional = optionalValue.asBoolean();
		}

		return optional;
	}

	private boolean determineOrphanRemoval(AnnotationInstance associationAnnotation) {
		boolean orphanRemoval = false;
		AnnotationValue orphanRemovalValue = associationAnnotation.value( "orphanRemoval" );
		if ( orphanRemovalValue != null ) {
			orphanRemoval = orphanRemovalValue.asBoolean();
		}
		return orphanRemoval;
	}

	private boolean determineFetchType(AnnotationInstance associationAnnotation) {
		boolean lazy = false;
		AnnotationValue fetchValue = associationAnnotation.value( "fetch" );
		if ( fetchValue != null ) {
			FetchType fetchType = Enum.valueOf( FetchType.class, fetchValue.asEnum() );
			if ( FetchType.LAZY.equals( fetchType ) ) {
				lazy = true;
			}
		}
		return lazy;
	}

	private String determineReferencedEntityType(AnnotationInstance associationAnnotation) {
		String targetTypeName = getAttributeType().getName();

		AnnotationInstance targetAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.TARGET
		);
		if ( targetAnnotation != null ) {
			targetTypeName = targetAnnotation.value().asClass().name().toString();
		}

		AnnotationValue targetEntityValue = associationAnnotation.value( "targetEntity" );
		if ( targetEntityValue != null ) {
			targetTypeName = targetEntityValue.asClass().name().toString();
		}

		return targetTypeName;
	}

	private String determineMappedByAttributeName(AnnotationInstance associationAnnotation) {
		String mappedBy = null;
		AnnotationValue mappedByAnnotationValue = associationAnnotation.value( "mappedBy" );
		if ( mappedByAnnotationValue != null ) {
			mappedBy = mappedByAnnotationValue.asString();
		}

		return mappedBy;
	}

	private Set<CascadeType> determineCascadeTypes(AnnotationInstance associationAnnotation) {
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

	private FetchMode determineFetchMode() {
		FetchMode mode = FetchMode.DEFAULT;

		AnnotationInstance fetchAnnotation = JandexHelper.getSingleAnnotation( annotations(), HibernateDotNames.FETCH );
		if ( fetchAnnotation != null ) {
			org.hibernate.annotations.FetchMode annotationFetchMode = JandexHelper.getEnumValue(
					fetchAnnotation,
					"value",
					org.hibernate.annotations.FetchMode.class
			);
			mode = EnumConversionHelper.annotationFetchModeToHibernateFetchMode( annotationFetchMode );
		}

		return mode;
	}

	private String determineMapsId() {
		String referencedIdAttributeName;
		AnnotationInstance mapsIdAnnotation = JandexHelper.getSingleAnnotation( annotations(), JPADotNames.MAPS_ID );
		if ( mapsIdAnnotation == null ) {
			return null;
		}

		if ( !( AttributeNature.MANY_TO_ONE.equals( getAssociationNature() ) || AttributeNature.MANY_TO_ONE
				.equals( getAssociationNature() ) ) ) {
			throw new MappingException(
					"@MapsId can only be specified on a many-to-one or one-to-one associations",
					getContext().getOrigin()
			);
		}

		referencedIdAttributeName = JandexHelper.getValue( mapsIdAnnotation, "value", String.class );

		return referencedIdAttributeName;
	}
}


