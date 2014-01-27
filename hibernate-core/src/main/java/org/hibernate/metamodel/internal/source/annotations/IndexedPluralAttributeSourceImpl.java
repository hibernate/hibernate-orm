/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.EnumSet;

import org.hibernate.AnnotationException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.internal.Binder;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceResolutionContext;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.IdentifierSource;
import org.hibernate.metamodel.spi.source.IndexedPluralAttributeSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.SimpleIdentifierSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class IndexedPluralAttributeSourceImpl extends PluralAttributeSourceImpl
		implements IndexedPluralAttributeSource {
	private PluralAttributeIndexSource indexSource;
	private final static EnumSet<MappedAttribute.Nature> VALID_NATURES = EnumSet.of(
			MappedAttribute.Nature.MANY_TO_MANY,
			MappedAttribute.Nature.ONE_TO_MANY,
			MappedAttribute.Nature.ELEMENT_COLLECTION_BASIC,
			MappedAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE);

	public IndexedPluralAttributeSourceImpl(
			final PluralAssociationAttribute attribute,
			final ConfiguredClass entityClass,
			final String relativePath) {
		super( attribute, entityClass, relativePath );
		if ( !VALID_NATURES.contains( attribute.getNature() ) ) {
			throw new MappingException(
					"Indexed column could be only mapped on the MANY side",
					attribute.getContext().getOrigin()
			);
		}
		
		if ( attribute.getPluralAttributeNature() == PluralAttributeSource.Nature.ARRAY
				&& !attribute.annotations().containsKey( JPADotNames.ORDER_COLUMN ) 
				&& !attribute.annotations().containsKey( HibernateDotNames.INDEX_COLUMN ) ) {
			throw new AnnotationException( "The array attribute '" + attribute.getRole()
					+ "' must be annotated with @OrderColumn or @IndexColumn!" );
		}
		
		if ( attribute.isSequentiallyIndexed() ) {
			final Binder.DefaultNamingStrategy defaultNamingStrategy = new Binder.DefaultNamingStrategy() {
				@Override
				public String defaultName(NamingStrategy namingStrategy) {
					return namingStrategy.propertyToColumnName( attribute.getName() ) + "_ORDER";
				}
			};
			indexSource = new SequentialPluralAttributeIndexSourceImpl( this, attribute, defaultNamingStrategy );
		}
		else if ( attribute.annotations().containsKey( JPADotNames.MAP_KEY ) ) {
			// need to wait until the ID or attribute source can be resolved.
			indexSource = null;
		}
		else if ( attribute.annotations().containsKey( JPADotNames.MAP_KEY_CLASS ) ) {
			// can be anything
			throw new NotYetImplementedException( "@MapKeyClass is not supported yet." );
		}
		//TODO the map attribute may contains both {@code MAP_KEY_COLUMN} and {@code MAP_KEY_CLASS}
		else if ( attribute.annotations().containsKey( JPADotNames.MAP_KEY_COLUMN ) ) {
			final Binder.DefaultNamingStrategy defaultNamingStrategy = new Binder.DefaultNamingStrategy() {
				@Override
				public String defaultName(NamingStrategy namingStrategy) {
					return namingStrategy.propertyToColumnName( attribute.getName() ) + "_KEY";
				}
			};
			indexSource = new BasicPluralAttributeIndexSourceImpl( this, attribute, defaultNamingStrategy );
		}
		else if ( attribute.annotations().containsKey( JPADotNames.MAP_KEY_ENUMERATED ) ) {
			// basic
			throw new NotYetImplementedException( "@MapKeyEnumerated is not supported yet." );
		}
		else if ( attribute.annotations().containsKey( JPADotNames.MAP_KEY_TEMPORAL ) ) {
			// basic
			throw new NotYetImplementedException( "@MapKeyTemporal is not supported yet." );
		}
		else if ( attribute.annotations().containsKey( JPADotNames.MAP_KEY_JOIN_COLUMN ) ) {
			// association
			throw new NotYetImplementedException( "@MapKeyJoinColumn is not supported yet." );
		}
		else if ( attribute.annotations().containsKey( JPADotNames.MAP_KEY_JOIN_COLUMNS ) ) {
			// association
			throw new NotYetImplementedException( "@MapKeyJoinColumns is not supported yet." );
		}
		else if ( String.class.equals( attribute.getIndexType() ) || attribute.getIndexType().isPrimitive() ) {
			final Binder.DefaultNamingStrategy defaultNamingStrategy = new Binder.DefaultNamingStrategy() {
				@Override
				public String defaultName(NamingStrategy namingStrategy) {
					return namingStrategy.propertyToColumnName( attribute.getName() ) + "_KEY";
				}
			};
			indexSource = new BasicPluralAttributeIndexSourceImpl( this, attribute, defaultNamingStrategy );
		}
		else {
			// either @Embeddable or entity type.

			// composite:
			// index is @Embeddable
			// @MapKeyClass is not basic, not entity type

			// association:
			// MapKeyJoinColumn, MapKeyJoinColumns are present
			// If the primary key of the referenced entity is not a simple primary key, must have MapKeyJoinColumns.
			//indexSource = new BasicPluralAttributeIndexSourceImpl( this, attribute );
			throw new NotYetImplementedException( "Embeddable and entity keys are not supported yet." );
		}
	}

	@Override
	public PluralAttributeIndexSource resolvePluralAttributeIndexSource(AttributeSourceResolutionContext attributeSourceResolutionContext) {
		if ( indexSource == null ) {
			if ( pluralAssociationAttribute().annotations().containsKey( JPADotNames.MAP_KEY ) ) {
				indexSource = resolveMapKeyPluralAttributeIndexSource( attributeSourceResolutionContext );
			}
			else {
				throw new NotYetImplementedException( "caonnot resolve index source." );
			}
		}
		return indexSource;
	}

	private PluralAttributeIndexSource resolveMapKeyPluralAttributeIndexSource(AttributeSourceResolutionContext attributeSourceResolutionContext) {
		final AnnotationInstance mapKeyAnnotation =
				JandexHelper.getSingleAnnotation( pluralAssociationAttribute().annotations(), JPADotNames.MAP_KEY );
		final String attributeName = JandexHelper.getValue( mapKeyAnnotation, "name", String.class,
				entityClass.getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		final PluralAttributeIndexSource innerIndexSource;
		if ( attributeName == null ) {
			IdentifierSource identifierSource = attributeSourceResolutionContext.resolveIdentifierSource(
							pluralAssociationAttribute().getReferencedEntityType()
			);
			switch ( identifierSource.getNature() ) {
				case SIMPLE:
					innerIndexSource = new BasicPluralAttributeIndexSourceImpl(
							this,
							pluralAssociationAttribute(),
							null,
							( (SimpleIdentifierSource) identifierSource ).getIdentifierAttributeSource().relationalValueSources() );
					break;
				default:
					throw new NotYetImplementedException( "Only simple IDs are supported for @MapKey" );
			}
		}
		else {
			AttributeSource attributeSource = attributeSourceResolutionContext.resolveAttributeSource(
					pluralAssociationAttribute().getReferencedEntityType(),
					attributeName
			);
			if ( ! attributeSource.isSingular() ) {
				throw new MappingException(
						String.format(
								"Plural attribute index [%s.%s] is not a singular attribute.",
								pluralAssociationAttribute().getReferencedEntityType(),
								attributeName
						),
						pluralAssociationAttribute().getContext().getOrigin()
				);
			}
			final SingularAttributeSource mapKeyAttributeSource = (SingularAttributeSource) attributeSource;
			switch ( mapKeyAttributeSource.getNature() ) {
				case BASIC:
					innerIndexSource = new BasicPluralAttributeIndexSourceImpl(
							this,
							pluralAssociationAttribute(),
							null,
							mapKeyAttributeSource.relationalValueSources() );
					break;
				case COMPOSITE:
					innerIndexSource = new CompositePluralAttributeIndexSourceImpl(
							pluralAssociationAttribute(),
							( ( ComponentAttributeSource) attributeSource  ).attributeSources(),
							null
					);
					break;
				default:
					throw new NotYetImplementedException( "Only basic plural attribute index sources are supported for @MapKey" );
			}
		}
		return new MapKeyPluralAttributeIndexSourceImpl( pluralAssociationAttribute(), innerIndexSource, attributeName );
	}

	@Override
	public PluralAttributeIndexSource getIndexSource() {
		return indexSource;
	}
}
