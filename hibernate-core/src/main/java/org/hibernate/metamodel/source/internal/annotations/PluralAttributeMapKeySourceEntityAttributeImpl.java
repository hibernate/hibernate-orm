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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.List;

import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.spi.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.AttributeSourceResolutionContext;
import org.hibernate.metamodel.source.spi.IdentifierSource;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.source.spi.PluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.PluralAttributeIndexSourceResolver;
import org.hibernate.metamodel.source.spi.PluralAttributeMapKeySourceEntityAttribute;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.SimpleIdentifierSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;

/**
 * Modeling of the JPA {@link javax.persistence.MapKey} annotation
 *
 * @author Steve Ebersole
 */
public class PluralAttributeMapKeySourceEntityAttributeImpl
		extends AbstractPluralAttributeIndexSourceImpl
		implements PluralAttributeMapKeySourceEntityAttribute, PluralAttributeIndexSourceResolver {

	private final String mapKeyAttributeName;
	private final Binder.DefaultNamingStrategy defaultNamingStrategy;
	private SingularAttributeSource mapKeyAttributeSource;

	public PluralAttributeMapKeySourceEntityAttributeImpl(
			PluralAttribute attribute,
			String mapKeyAttributeName) {
		super( attribute );
		this.mapKeyAttributeName = mapKeyAttributeName;
		this.defaultNamingStrategy = new PluralAttributeMapSourceImpl.MapKeyColumnDefaultNaming( attribute );
	}

	@Override
	public PluralAttributeIndexSource resolvePluralAttributeIndexSource(AttributeSourceResolutionContext context) {
		if ( mapKeyAttributeName == null ) {
			final IdentifierSource identifierSource = context.resolveIdentifierSource(
					pluralAttribute().getElementDetails().getJavaType().getName().toString()
			);
			switch ( identifierSource.getNature() ) {
				case SIMPLE: {
					mapKeyAttributeSource = ( (SimpleIdentifierSource) identifierSource ).getIdentifierAttributeSource();
					break;
				}
				case AGGREGATED_COMPOSITE: {
					mapKeyAttributeSource = ( (AggregatedCompositeIdentifierSource) identifierSource ).getIdentifierAttributeSource();
				}
				default: {
					throw pluralAttribute().getContext().makeMappingException(
							"Non-aggregated composite identifiers are not supported for @MapKey"
					);
				}
			}
		}
		else {
			AttributeSource attributeSource = context.resolveAttributeSource(
					pluralAttribute().getElementDetails().getJavaType().getName().toString(),
					mapKeyAttributeName
			);
			if ( ! attributeSource.isSingular() ) {
				throw new MappingException(
						String.format(
								"Plural attribute index [%s.%s] is not a singular attribute.",
								pluralAttribute().getElementDetails().getJavaType().getName().toString(),
								mapKeyAttributeName
						),
						pluralAttribute().getContext().getOrigin()
				);
			}
			mapKeyAttributeSource = (SingularAttributeSource) attributeSource;
		}

		return this;
	}

	@Override
	public String getAttributeName() {
		return mapKeyAttributeName;
	}

	@Override
	public PluralAttributeIndexNature getNature() {
		if ( mapKeyAttributeSource == null ) {
			return null;
		}

		switch ( mapKeyAttributeSource.getSingularAttributeNature() ) {
			case BASIC: {
				return PluralAttributeIndexNature.BASIC;
			}
			case COMPOSITE: {
				return PluralAttributeIndexNature.AGGREGATE;
			}
			case ANY: {
				return PluralAttributeIndexNature.MANY_TO_ANY;
			}
			case MANY_TO_ONE: {
				return PluralAttributeIndexNature.MANY_TO_MANY;
			}
			default: {
				throw pluralAttribute().getContext().makeMappingException(
						"Unexpected attribute nature : " + mapKeyAttributeSource.getSingularAttributeNature()
				);
			}
		}
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies() {
		return null;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		if ( mapKeyAttributeSource == null ) {
			return null;
		}
		return mapKeyAttributeSource.relationalValueSources();
	}
}
