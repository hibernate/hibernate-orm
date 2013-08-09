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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class SourceHelper {

	public static ValueHolder<List<AttributeSource>> resolveAttributes(
			final ConfiguredClass configuredClass,
			final String relativePath,
			final Map<String, AttributeOverride> attributeOverrideMap,
			final Map<String, AssociationOverride> associationAttributeMap){
		return new ValueHolder<List<AttributeSource>>(
				new ValueHolder.DeferredInitializer<List<AttributeSource>>() {
					@Override
					public List<AttributeSource> initialize() {
						List<AttributeSource> attributeList = new ArrayList<AttributeSource>();
						for ( BasicAttribute attribute : configuredClass.getSimpleAttributes().values() ) {
							SingularAttributeSourceImpl source = new SingularAttributeSourceImpl( attribute, relativePath );
							attributeList.add( source );
						}

						for ( Map.Entry<String, EmbeddableClass> entry : configuredClass.getEmbeddedClasses()
								.entrySet() ) {
							final String attributeName = entry.getKey();
							if ( !configuredClass.isIdAttribute( attributeName ) ) {
								final EmbeddableClass component = entry.getValue();
								ComponentAttributeSourceImpl source = new ComponentAttributeSourceImpl(
										component,
										relativePath,
										configuredClass.getClassAccessType()
								);
								attributeList.add( source );
							}
						}
						resolveAssociationAttributes( configuredClass, attributeList, relativePath );
						for ( AttributeSource attributeSource : attributeList ) {
							if ( attributeSource instanceof AnnotationAttributeSource ) {
								AnnotationAttributeSource source = (AnnotationAttributeSource) attributeSource;
								source.applyAssociationOverride(
										associationAttributeMap != null ? associationAttributeMap : Collections
												.<String, AssociationOverride>emptyMap()
								);
								source.applyAttributeOverride(
										attributeOverrideMap != null ? attributeOverrideMap : Collections.<String, AttributeOverride>emptyMap()
								);
							}
						}
						return attributeList;
					}
				}
		);
	}

	public static ValueHolder<List<AttributeSource>> resolveAttributes(final ConfiguredClass configuredClass, final String relativePath){
		return resolveAttributes( configuredClass, relativePath, configuredClass.getAttributeOverrideMap(), configuredClass.getAssociationOverrideMap() );

	}

	/**
	 * Bind association attributes within {@param configuredClass} to the proper source impl based on its nature.
	 *
	 * @param configuredClass The holder of association attributes.
	 * @param attributeList Attribute source container, can't be <code>null</code>.
	 */
	private static void resolveAssociationAttributes(ConfiguredClass configuredClass, List<AttributeSource> attributeList, final String relativePath) {
		for ( AssociationAttribute associationAttribute : configuredClass.getAssociationAttributes().values() ) {
			switch ( associationAttribute.getNature() ) {
				case ONE_TO_ONE:
				case MANY_TO_ONE: {
					final SingularAssociationAttribute singularAssociationAttribute =
							(SingularAssociationAttribute) associationAttribute;
					final SingularAttributeSource source =
							associationAttribute.getMappedBy() == null ?
									new ToOneAttributeSourceImpl( singularAssociationAttribute, relativePath,
											configuredClass.getLocalBindingContext() ) :
									new ToOneMappedByAttributeSourceImpl( singularAssociationAttribute, relativePath );
					attributeList.add( source );
					break;
				}
				case MANY_TO_MANY:
				case ONE_TO_MANY:
				case ELEMENT_COLLECTION_BASIC:
				case ELEMENT_COLLECTION_EMBEDDABLE: {
					attributeList.add(
							createPluralAttributeSource(
									configuredClass,
									(PluralAssociationAttribute) associationAttribute,
									relativePath
							)
					);
					break;
				}
				default: {
					throw new NotYetImplementedException();
				}
			}
		}
	}

	private static PluralAttributeSource createPluralAttributeSource(
			ConfiguredClass configuredClass,
			PluralAssociationAttribute pluralAssociationAttribute,
			String relativePath) {
		switch ( pluralAssociationAttribute.getPluralAttributeNature() ) {
			case BAG: // fall through intentionally
			case SET: {
				return new PluralAttributeSourceImpl( pluralAssociationAttribute, configuredClass, relativePath );
			}
			case ARRAY:  // fall through intentionally
			case MAP:  // fall through intentionally
			case LIST: {
				return new IndexedPluralAttributeSourceImpl( pluralAssociationAttribute, configuredClass, relativePath );
			}
			case ID_BAG: {
				throw new NotYetImplementedException(
						String.format(
								"%s attributes are not supported yet",
								pluralAssociationAttribute.getPluralAttributeNature()
						)
				);
			}
			default: {
				throw new AssertionFailure(
						String.format(
								"Unknown plural attribute nature: %s",
								pluralAssociationAttribute.getPluralAttributeNature()
						)
				);
			}
		}

	}
}
