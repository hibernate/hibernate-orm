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

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.source.internal.annotations.attribute.OverrideAndConverterCollector;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttributeIndexDetails;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttributeIndexDetailsMapKeyBasic;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttributeIndexDetailsMapKeyEmbedded;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttributeIndexDetailsMapKeyEntityAttribute;
import org.hibernate.metamodel.source.spi.IndexedPluralAttributeSource;
import org.hibernate.metamodel.source.spi.PluralAttributeIndexSource;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeMapSourceImpl
		extends PluralAttributeSourceImpl
		implements IndexedPluralAttributeSource {
	private final PluralAttributeIndexSource mapKeySource;

	public PluralAttributeMapSourceImpl(
			PluralAttribute attribute,
			OverrideAndConverterCollector overrideAndConverterCollector) {
		super( attribute, overrideAndConverterCollector );

		this.mapKeySource = determineMapKeySourceInfo( attribute );
	}

	protected PluralAttributeIndexSource determineMapKeySourceInfo(final PluralAttribute attribute) {
		final PluralAttributeIndexDetails mapKeyDetails = attribute.getIndexDetails();

		if ( mapKeyDetails.getIndexNature() == null ) {
			return new PluralAttributeMapKeySourceEntityAttributeImpl(
					attribute,
					( (PluralAttributeIndexDetailsMapKeyEntityAttribute) mapKeyDetails ).getReferencedAttributeName()
			);
		}

		switch ( mapKeyDetails.getIndexNature() ) {
			case BASIC: {
				return new PluralAttributeMapKeySourceBasicImpl(
						attribute,
						(PluralAttributeIndexDetailsMapKeyBasic) mapKeyDetails
				);
			}
			case AGGREGATE: {
				return new PluralAttributeMapKeySourceEmbeddedImpl(
						attribute,
						(PluralAttributeIndexDetailsMapKeyEmbedded) mapKeyDetails
				);
			}
			default: {
				throw attribute.getContext().makeMappingException(
						"Support for entities as map keys is not yet implemented"
				);
			}
		}
	}

	@Override
	public PluralAttributeIndexSource getIndexSource() {
		return mapKeySource;
	}

	static class MapKeyColumnDefaultNaming implements Binder.DefaultNamingStrategy {
		private final PluralAttribute pluralAttribute;

		MapKeyColumnDefaultNaming(PluralAttribute pluralAttribute) {
			this.pluralAttribute = pluralAttribute;
		}

		@Override
		public String defaultName(NamingStrategy namingStrategy) {
			return pluralAttribute.getName() + "_KEY";
		}
	}

}
