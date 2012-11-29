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

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.spi.source.OneToManyPluralAttributeElementSource;

/**
 * @author Hardy Ferentschik
 */
public class OneToManyPluralAttributeElementSourceImpl implements OneToManyPluralAttributeElementSource {
	private final PluralAssociationAttribute associationAttribute;

	public OneToManyPluralAttributeElementSourceImpl(PluralAssociationAttribute associationAttribute) {
		this.associationAttribute = associationAttribute;
	}

	@Override
	public String getReferencedEntityName() {
		return associationAttribute.getReferencedEntityType();
	}

	@Override
	public boolean isNotFoundAnException() {
		return !associationAttribute.isIgnoreNotFound();
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		return EnumConversionHelper.cascadeTypeToCascadeStyleSet(
				associationAttribute.getCascadeTypes(),
				associationAttribute.getHibernateCascadeTypes(),
				associationAttribute.getContext() );
	}

	@Override
	public Nature getNature() {
		switch ( associationAttribute.getNature() ) {
			case MANY_TO_MANY: {
				return Nature.MANY_TO_MANY;
			}
			case MANY_TO_ANY: {
				return Nature.MANY_TO_ANY;
			}
			case ONE_TO_MANY: {
				return Nature.ONE_TO_MANY;
			}
			case ELEMENT_COLLECTION_BASIC: {
				return Nature.BASIC;
			}
			case ELEMENT_COLLECTION_EMBEDDABLE: {
				return Nature.AGGREGATE;
			}
			default: {
				throw new AssertionFailure( "Unexpected attribute nature: " + associationAttribute.getNature() );
			}
		}
	}

}


