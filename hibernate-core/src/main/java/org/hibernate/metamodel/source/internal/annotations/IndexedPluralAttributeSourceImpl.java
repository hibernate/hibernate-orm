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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.EnumSet;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.OverrideAndConverterCollector;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.AttributeSourceResolutionContext;
import org.hibernate.metamodel.source.spi.ComponentAttributeSource;
import org.hibernate.metamodel.source.spi.IdentifierSource;
import org.hibernate.metamodel.source.spi.IndexedPluralAttributeSource;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.source.spi.PluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.PluralAttributeSource;
import org.hibernate.metamodel.source.spi.SimpleIdentifierSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;

import org.jboss.jandex.AnnotationInstance;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class IndexedPluralAttributeSourceImpl extends PluralAttributeSourceImpl
		implements IndexedPluralAttributeSource {

	private final static EnumSet<AbstractPersistentAttribute.Nature> VALID_NATURES = EnumSet.of(
			AbstractPersistentAttribute.Nature.MANY_TO_MANY,
			AbstractPersistentAttribute.Nature.ONE_TO_MANY,
			AbstractPersistentAttribute.Nature.ELEMENT_COLLECTION_BASIC,
			AbstractPersistentAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE
	);

	private PluralAttributeIndexSource indexSource;

	public IndexedPluralAttributeSourceImpl(
			PluralAttribute attribute,
			OverrideAndConverterCollector overrideAndConverterCollector) {
		super( attribute, overrideAndConverterCollector );
		if ( !VALID_NATURES.contains( attribute.getNature() ) ) {
			throw new MappingException(
					"Indexed column could be only mapped on the MANY side",
					attribute.getContext().getOrigin()
			);
		}

		if ( attribute.getPluralAttributeNature() == PluralAttributeSource.Nature.ARRAY
				&& !attribute.getBackingMember().getAnnotations().containsKey( JPADotNames.ORDER_COLUMN ) ) {
			throw attribute.getContext().makeMappingException(
					"Persistent arrays must be annotated with @OrderColumn : " + attribute.getRole()
			);
		}

		this.indexSource = determineIndexSourceInfo( attribute );
	}

	private PluralAttributeIndexSource determineIndexSourceInfo(final PluralAttribute attribute) {
		// could be an array/list
		if ( attribute.getPluralAttributeNature() == Nature.ARRAY
				|| attribute.getPluralAttributeNature() == Nature.LIST ) {
			final Binder.DefaultNamingStrategy defaultNamingStrategy = new Binder.DefaultNamingStrategy() {
				@Override
				public String defaultName(NamingStrategy namingStrategy) {
					return namingStrategy.propertyToColumnName( attribute.getName() ) + "_ORDER";
				}
			};
			return new SequentialPluralAttributeIndexSourceImpl( this, attribute, defaultNamingStrategy );
		}

		// or a map
		return determineMapKeyInfo( attribute );
	}

	private PluralAttributeIndexSource determineMapKeyInfo(final PluralAttribute attribute) {
		final AnnotationInstance mapKey = attribute.getBackingMember().getAnnotations().get( JPADotNames.MAP_KEY );
		final AnnotationInstance mapKeyClass = attribute.getBackingMember().getAnnotations().get( JPADotNames.MAP_KEY_CLASS );

		if ( mapKey != null && mapKeyClass != null ) {
			// this is an error according to the spec...
			throw attribute.getContext().makeMappingException(
					"Map attribute [" + attribute.getName() + "] defined both " +
							"@MapKey and @MapKeyClass; only one should be used"
			);
		}

		if ( mapKey != null ) {
			// need to wait until the ID or attribute source can be resolved.
			return null;
		}

		if ( mapKeyClass != null ) {
			throw new NotYetImplementedException( "@MapKeyClass is not supported yet." );
		}


		final AnnotationInstance mapKeyColumn = attribute.getBackingMember().getAnnotations().get( JPADotNames.MAP_KEY_COLUMN );
		if ( mapKeyColumn != null ) {
			// todo : does this cover @MapKeyType???
			final Binder.DefaultNamingStrategy defaultNamingStrategy = new Binder.DefaultNamingStrategy() {
				@Override
				public String defaultName(NamingStrategy namingStrategy) {
					return namingStrategy.propertyToColumnName( attribute.getName() ) + "_KEY";
				}
			};
			return new BasicPluralAttributeIndexSourceImpl( this, attribute, defaultNamingStrategy );
		}


		if ( attribute.getBackingMember().getAnnotations().containsKey( JPADotNames.MAP_KEY_ENUMERATED ) ) {
			// basic
			throw new NotYetImplementedException( "@MapKeyEnumerated is not supported yet." );
		}
		else if ( attribute.getBackingMember().getAnnotations().containsKey( JPADotNames.MAP_KEY_TEMPORAL ) ) {
			// basic
			throw new NotYetImplementedException( "@MapKeyTemporal is not supported yet." );
		}
		else if ( attribute.getBackingMember().getAnnotations().containsKey( JPADotNames.MAP_KEY_JOIN_COLUMN ) ) {
			// association
			throw new NotYetImplementedException( "@MapKeyJoinColumn is not supported yet." );
		}
		else if ( attribute.getBackingMember().getAnnotations().containsKey( JPADotNames.MAP_KEY_JOIN_COLUMNS ) ) {
			// association
			throw new NotYetImplementedException( "@MapKeyJoinColumns is not supported yet." );
		}

		// todo : some of these in general ought to move to the attribute.getIndexDetails()


		// default, just assume the key is a "basic" type
		final Binder.DefaultNamingStrategy defaultNamingStrategy = new Binder.DefaultNamingStrategy() {
			@Override
			public String defaultName(NamingStrategy namingStrategy) {
				return namingStrategy.propertyToColumnName( attribute.getName() ) + "_KEY";
			}
		};
		return new BasicPluralAttributeIndexSourceImpl( this, attribute, defaultNamingStrategy );
	}

	@Override
	public PluralAttributeIndexSource resolvePluralAttributeIndexSource(AttributeSourceResolutionContext attributeSourceResolutionContext) {
		if ( indexSource == null ) {
			final AnnotationInstance mapKey = pluralAssociationAttribute().getBackingMember().getAnnotations().get( JPADotNames.MAP_KEY );
			if ( mapKey != null ) {
				indexSource = resolveMapKeyPluralAttributeIndexSource( attributeSourceResolutionContext, mapKey );
			}
			else {
				throw new NotYetImplementedException( "cannot resolve index source." );
			}
		}
		return indexSource;
	}

	private PluralAttributeIndexSource resolveMapKeyPluralAttributeIndexSource(
			AttributeSourceResolutionContext attributeSourceResolutionContext,
			AnnotationInstance mapKey) {
		final String attributeName = mapKey.value( "name" ).asString();
		final PluralAttributeIndexSource innerIndexSource;
		if ( attributeName == null ) {
			final IdentifierSource identifierSource = attributeSourceResolutionContext.resolveIdentifierSource(
					pluralAssociationAttribute().getElementDetails().getJavaType().getName().toString()
			);
			switch ( identifierSource.getNature() ) {
				case SIMPLE: {
					innerIndexSource = new BasicPluralAttributeIndexSourceImpl(
							this,
							pluralAssociationAttribute(),
							null,
							( (SimpleIdentifierSource) identifierSource ).getIdentifierAttributeSource().relationalValueSources() );
					break;
				}
				default: {
					throw new NotYetImplementedException( "Only simple IDs are supported for @MapKey" );
				}
			}
		}
		else {
			AttributeSource attributeSource = attributeSourceResolutionContext.resolveAttributeSource(
					pluralAssociationAttribute().getElementDetails().getJavaType().getName().toString(),
					attributeName
			);
			if ( ! attributeSource.isSingular() ) {
				throw new MappingException(
						String.format(
								"Plural attribute index [%s.%s] is not a singular attribute.",
								pluralAssociationAttribute().getElementDetails().getJavaType().getName().toString(),
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
							( (ComponentAttributeSource) attributeSource  ).attributeSources(),
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
