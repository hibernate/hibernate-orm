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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.EmbeddedAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.OverrideAndConverterCollector;
import org.hibernate.metamodel.source.internal.annotations.attribute.PersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.IdentifiableTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.MappedSuperclassTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.RootEntityTypeMetadata;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.EmbeddedAttributeSource;
import org.hibernate.metamodel.source.spi.MapsIdSource;
import org.hibernate.metamodel.source.spi.PluralAttributeSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;

import org.jboss.jandex.AnnotationValue;

/**
 * Utilities for building attribute source objects.
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class SourceHelper {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SourceHelper.class );

	// todo : the walking supers bits here is due to the total lack of understanding of MappedSuperclasses in Binder...

	public static List<AttributeSource> buildAttributeSources(
			ManagedTypeMetadata managedTypeMetadata,
			AttributeBuilder attributeBuilder) {
		final List<AttributeSource> result = new ArrayList<AttributeSource>();

		if ( EntityTypeMetadata.class.isInstance( managedTypeMetadata ) ) {
			final EntityTypeMetadata entityTypeMetadata = (EntityTypeMetadata) managedTypeMetadata;

			IdentifiableTypeMetadata currentSuperType = entityTypeMetadata.getSuperType();

			while ( currentSuperType != null && MappedSuperclassTypeMetadata.class.isInstance( currentSuperType ) ) {
				collectAttributeSources( result, currentSuperType, entityTypeMetadata, attributeBuilder );

				currentSuperType = currentSuperType.getSuperType();
			}
		}

		collectAttributeSources( result, managedTypeMetadata, managedTypeMetadata, attributeBuilder );

		return result;
	}

	private static void collectAttributeSources(
			List<AttributeSource> result,
			ManagedTypeMetadata managedTypeMetadata,
			OverrideAndConverterCollector overrideAndConverterCollector,
			AttributeBuilder attributeBuilder) {
		for ( PersistentAttribute attribute : managedTypeMetadata.getPersistentAttributeMap().values() ) {
			switch ( attribute.getNature() ) {
				case BASIC: {
					result.add(
							attributeBuilder.buildBasicAttribute(
									(BasicAttribute) attribute,
									overrideAndConverterCollector
							)
					);
					break;
				}
				case EMBEDDED: {
					result.add(
							attributeBuilder.buildEmbeddedAttribute(
									(EmbeddedAttribute) attribute,
									overrideAndConverterCollector
							)
					);
					break;
				}
				case MANY_TO_ONE:
				case ONE_TO_ONE: {
					result.add(
							attributeBuilder.buildToOneAttribute(
									(SingularAssociationAttribute) attribute,
									overrideAndConverterCollector
							)
					);
					break;
				}
				case MANY_TO_MANY:
				case ONE_TO_MANY:
				case ELEMENT_COLLECTION_BASIC:
				case ELEMENT_COLLECTION_EMBEDDABLE: {
					result.add(
							attributeBuilder.buildPluralAttribute(
									(PluralAttribute) attribute,
									overrideAndConverterCollector
							)
					);
					break;
				}
				case ANY: {
					result.add(
							attributeBuilder.buildAnyAttribute(
									attribute,
									overrideAndConverterCollector
							)
					);
					break;
				}
				case MANY_TO_ANY: {
					result.add(
							attributeBuilder.buildManyToAnyAttribute(
									attribute,
									overrideAndConverterCollector
							)
					);
					break;
				}
				default: {
					throw managedTypeMetadata.getLocalBindingContext().makeMappingException(
							"Unexpected PersistentAttribute nature encountered : " + attribute.getNature()
					);
				}
			}
		}
	}

	public static List<SingularAttributeSource> buildIdentifierAttributeSources(
			RootEntityTypeMetadata rootEntityTypeMetadata,
			AttributeBuilder attributeBuilder) {
		final List<SingularAttributeSource> result = new ArrayList<SingularAttributeSource>();

// we already specially collect identifier attributes
//		IdentifiableTypeMetadata currentSuperType = rootEntityTypeMetadata.getSuperType();
//		while ( currentSuperType != null && MappedSuperclassTypeMetadata.class.isInstance( currentSuperType ) ) {
//			collectIdentifierAttributeSources( result, currentSuperType, rootEntityTypeMetadata, attributeBuilder );
//
//			currentSuperType = currentSuperType.getSuperType();
//		}

		collectIdentifierAttributeSources( result, rootEntityTypeMetadata, rootEntityTypeMetadata, attributeBuilder );

		return result;
	}

	private static void collectIdentifierAttributeSources(
			List<SingularAttributeSource> result,
			IdentifiableTypeMetadata identifiableTypeMetadata,
			OverrideAndConverterCollector overrideAndConverterCollector,
			AttributeBuilder attributeBuilder) {
		for ( PersistentAttribute attribute : identifiableTypeMetadata.getIdentifierAttributes() ) {
			switch ( attribute.getNature() ) {
				case BASIC: {
					result.add(
							attributeBuilder.buildBasicAttribute(
									(BasicAttribute) attribute,
									overrideAndConverterCollector
							)
					);
					break;
				}
				case EMBEDDED:
				case EMBEDDED_ID: {
					result.add(
							attributeBuilder.buildEmbeddedAttribute(
									(EmbeddedAttribute) attribute,
									overrideAndConverterCollector
							)
					);
					break;
				}
				case MANY_TO_ONE:
				case ONE_TO_ONE: {
					result.add(
							attributeBuilder.buildToOneAttribute(
									(SingularAssociationAttribute) attribute,
									overrideAndConverterCollector
							)
					);
					break;
				}
				case MANY_TO_MANY:
				case ONE_TO_MANY:
				case ELEMENT_COLLECTION_BASIC:
				case ELEMENT_COLLECTION_EMBEDDABLE: {
					throw identifiableTypeMetadata.getLocalBindingContext().makeMappingException(
							"Plural attribute cannot be part of identifier : " + attribute.getBackingMember().toString()
					);
				}
				case ANY: {
					throw identifiableTypeMetadata.getLocalBindingContext().makeMappingException(
							"Hibernate ANY mapping cannot be part of identifier : " + attribute.getBackingMember().toString()
					);
				}
				case MANY_TO_ANY: {
					throw identifiableTypeMetadata.getLocalBindingContext().makeMappingException(
							"Hibernate MANY-TO-ANY mapping cannot be part of identifier : " + attribute.getBackingMember().toString()
					);
				}
				default: {
					throw identifiableTypeMetadata.getLocalBindingContext().makeMappingException(
							"Unexpected PersistentAttribute nature encountered : " + attribute.getNature()
					);
				}
			}
		}
	}

	public static List<MapsIdSource> buildMapsIdSources(
			RootEntityTypeMetadata entityTypeMetadata,
			IdentifierPathAttributeBuilder attributeBuilder) {
		final List<MapsIdSource> result = new ArrayList<MapsIdSource>();
		for ( final SingularAssociationAttribute attribute : entityTypeMetadata.getMapsIdAttributes() ) {
			final ToOneAttributeSource attributeSource = attributeBuilder.buildToOneAttribute(
					attribute,
					entityTypeMetadata
			);

			final AnnotationValue mapsIdNameValue = attribute.getMapsIdAnnotation().value();
			final String mappedIdAttributeName = mapsIdNameValue == null
					? null
					: StringHelper.nullIfEmpty( mapsIdNameValue.asString() );
			result.add(
					new MapsIdSource() {
						@Override
						public String getMappedIdAttributeName() {
							return mappedIdAttributeName;
						}

						@Override
						public ToOneAttributeSource getAssociationAttributeSource() {
							return attributeSource;
						}
					}
			);
		}
		return result;
	}

	public static interface AttributeBuilder {
		public SingularAttributeSource buildBasicAttribute(
				BasicAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector);

		public EmbeddedAttributeSource buildEmbeddedAttribute(
				EmbeddedAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector);

		public ToOneAttributeSource buildToOneAttribute(
				SingularAssociationAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector);

		public PluralAttributeSource buildPluralAttribute(
				PluralAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector);

		public AttributeSource buildAnyAttribute(
				PersistentAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector);

		public PluralAttributeSource buildManyToAnyAttribute(
				PersistentAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector);
	}


	public static class StandardAttributeBuilder implements AttributeBuilder {
		/**
		 * Singleton access
		 */
		public static final StandardAttributeBuilder INSTANCE = new StandardAttributeBuilder();

		@Override
		public SingularAttributeSource buildBasicAttribute(
				BasicAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			return new BasicAttributeSourceImpl( attribute, overrideAndConverterCollector );
		}

		@Override
		public EmbeddedAttributeSource buildEmbeddedAttribute(
				EmbeddedAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			return new EmbeddedAttributeSourceImpl( attribute, false, false );
		}

		@Override
		public ToOneAttributeSource buildToOneAttribute(
				SingularAssociationAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			if ( attribute.getMappedByAttributeName() == null ) {
				return new ToOneAttributeSourceImpl( attribute, overrideAndConverterCollector );
			}
			else {
				return new ToOneMappedByAttributeSourceImpl( attribute, overrideAndConverterCollector );
			}
		}

		@Override
		public PluralAttributeSource buildPluralAttribute(
				PluralAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			switch ( attribute.getPluralAttributeNature() ) {
				case BAG:
				case SET: {
					return new PluralAttributeSourceImpl( attribute, overrideAndConverterCollector );
				}
				case ID_BAG: {
					return new PluralAttributeIdBagSourceImpl( attribute, overrideAndConverterCollector );
				}
				case MAP: {
					return new PluralAttributeMapSourceImpl( attribute, overrideAndConverterCollector );
				}
				case ARRAY:
				case LIST: {
					return new PluralAttributeIndexedSourceImpl( attribute, overrideAndConverterCollector );
				}
				default: {
					throw new AssertionFailure(
							String.format(
									Locale.ENGLISH,
									"Unknown or not-yet-supported plural attribute nature: %s",
									attribute.getPluralAttributeNature()
							)
					);
				}
			}
		}

		@Override
		public AttributeSource buildAnyAttribute(
				PersistentAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			throw new NotYetImplementedException();
		}

		@Override
		public PluralAttributeSource buildManyToAnyAttribute(
				PersistentAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			throw new NotYetImplementedException();
		}
	}

	public static class PluralAttributesDisallowedAttributeBuilder extends StandardAttributeBuilder {
		/**
		 * Singleton access
		 */
		public static final PluralAttributesDisallowedAttributeBuilder INSTANCE = new PluralAttributesDisallowedAttributeBuilder();

		@Override
		public EmbeddedAttributeSource buildEmbeddedAttribute(
				EmbeddedAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			return new EmbeddedAttributeSourceImpl( attribute, false, true );
		}

		@Override
		public PluralAttributeSource buildPluralAttribute(
				PluralAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			throw attribute.getContext().makeMappingException(
					"Plural attributes not allowed in this context : " + attribute.getBackingMember().toString()
			);
		}

		@Override
		public PluralAttributeSource buildManyToAnyAttribute(
				PersistentAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			throw attribute.getContext().makeMappingException(
					"Plural attributes not allowed in this context : " + attribute.getBackingMember().toString()
			);
		}
	}

	public static class IdentifierPathAttributeBuilder extends PluralAttributesDisallowedAttributeBuilder {
		/**
		 * Singleton access
		 */
		public static final IdentifierPathAttributeBuilder INSTANCE = new IdentifierPathAttributeBuilder();

		@Override
		public PluralAttributeSource buildPluralAttribute(
				PluralAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			throw attribute.getContext().makeMappingException(
					"Plural attribute cannot be part of identifier : " + attribute.getBackingMember().toString()
			);
		}

		@Override
		public PluralAttributeSource buildManyToAnyAttribute(
				PersistentAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			throw attribute.getContext().makeMappingException(
					"Plural attribute cannot be part of identifier : " + attribute.getBackingMember().toString()
			);
		}

		@Override
		public AttributeSource buildAnyAttribute(
				PersistentAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			throw attribute.getContext().makeMappingException(
					"Any mapping cannot be part of identifier : " + attribute.getBackingMember().toString()
			);
		}

		@Override
		public EmbeddedAttributeSource buildEmbeddedAttribute(
				EmbeddedAttribute attribute,
				OverrideAndConverterCollector overrideAndConverterCollector) {
			return new EmbeddedAttributeSourceImpl( attribute, true, false );
		}
	}

}
