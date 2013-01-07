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

import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.spi.source.IndexedPluralAttributeSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class IndexedPluralAttributeSourceImpl extends PluralAttributeSourceImpl
		implements IndexedPluralAttributeSource {
	private final PluralAttributeIndexSource indexSource;
	private final static EnumSet<MappedAttribute.Nature> VALID_NATURES = EnumSet.of(
			MappedAttribute.Nature.MANY_TO_MANY,
			MappedAttribute.Nature.ONE_TO_MANY,
			MappedAttribute.Nature.ELEMENT_COLLECTION_BASIC,
			MappedAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE);

	public IndexedPluralAttributeSourceImpl(PluralAssociationAttribute attribute,
			ConfiguredClass entityClass ) {
		super( attribute, entityClass );
		if ( !VALID_NATURES.contains( attribute.getNature() ) ) {
			throw new MappingException(
					"Indexed column could be only mapped on the MANY side",
					attribute.getContext().getOrigin()
			);
		}
		if ( attribute.isSequentiallyIndexed() ) {
			indexSource = new SequentialPluralAttributeIndexSourceImpl( this, attribute );
		}
		else {
			// for now assume the index is basic type
			indexSource = new BasicPluralAttributeIndexSourceImpl( this, attribute );
		}
	}

	@Override
	public PluralAttributeIndexSource getIndexSource() {
		return indexSource;
	}
}
