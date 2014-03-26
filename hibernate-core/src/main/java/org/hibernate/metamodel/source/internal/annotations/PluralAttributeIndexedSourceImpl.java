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

import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.OverrideAndConverterCollector;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.spi.IndexedPluralAttributeSource;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.source.spi.PluralAttributeIndexSource;
import org.hibernate.metamodel.spi.PluralAttributeNature;

/**
 * @author Strong Liu
 * @author Steve Ebersole
 */
public class PluralAttributeIndexedSourceImpl
		extends PluralAttributeSourceImpl
		implements IndexedPluralAttributeSource {

	private final static EnumSet<AbstractPersistentAttribute.Nature> VALID_NATURES = EnumSet.of(
			AbstractPersistentAttribute.Nature.MANY_TO_MANY,
			AbstractPersistentAttribute.Nature.ONE_TO_MANY,
			AbstractPersistentAttribute.Nature.ELEMENT_COLLECTION_BASIC,
			AbstractPersistentAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE
	);

	private PluralAttributeIndexSource indexSource;

	public PluralAttributeIndexedSourceImpl(
			PluralAttribute attribute,
			OverrideAndConverterCollector overrideAndConverterCollector) {
		super( attribute, overrideAndConverterCollector );
		if ( !VALID_NATURES.contains( attribute.getNature() ) ) {
			throw new MappingException(
					"Indexed column could be only mapped on the MANY side",
					attribute.getContext().getOrigin()
			);
		}

		if ( attribute.getPluralAttributeNature() == PluralAttributeNature.ARRAY
				&& !attribute.getBackingMember().getAnnotations().containsKey( JPADotNames.ORDER_COLUMN ) ) {
			throw attribute.getContext().makeMappingException(
					"Persistent arrays must be annotated with @OrderColumn : " + attribute.getRole()
			);
		}

		this.indexSource = new PluralAttributeSequentialIndexSourceImpl( attribute );
	}


	@Override
	public PluralAttributeIndexSource getIndexSource() {
		return indexSource;
	}
}
