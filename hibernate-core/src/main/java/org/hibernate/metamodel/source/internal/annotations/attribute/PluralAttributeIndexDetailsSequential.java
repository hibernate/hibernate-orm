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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import static org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames.LIST_INDEX_BASE;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.ORDER_COLUMN;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexDetailsSequential implements PluralAttributeIndexDetails {
	private final PluralAttribute pluralAttribute;

	private final Column orderColumn;
	private final int base;

	public PluralAttributeIndexDetailsSequential(PluralAttribute pluralAttribute, MemberDescriptor backingMember) {
		this.pluralAttribute = pluralAttribute;

		this.orderColumn = determineOrderColumn( backingMember );
		this.base = determineIndexBase( backingMember );
	}

	private Column determineOrderColumn(MemberDescriptor backingMember) {
		final AnnotationInstance orderColumnAnnotation = backingMember.getAnnotations().get( ORDER_COLUMN );
		if ( orderColumnAnnotation == null ) {
			return null;
		}
		return new Column( orderColumnAnnotation );
	}

	private int determineIndexBase(MemberDescriptor backingMember) {
		final AnnotationInstance listIndexBase = backingMember.getAnnotations().get( LIST_INDEX_BASE );
		if ( listIndexBase == null ) {
			return 0;
		}

		final AnnotationValue baseValue = listIndexBase.value();
		if ( baseValue == null ) {
			return 0;
		}

		return baseValue.asInt();
	}

	public Column getOrderColumn() {
		return orderColumn;
	}

	public int getBase() {
		return base;
	}

	@Override
	public JavaTypeDescriptor getJavaType() {
		return null;
	}

	@Override
	public PluralAttributeIndexNature getIndexNature() {
		return PluralAttributeIndexNature.SEQUENTIAL;
	}

	@Override
	public AttributeTypeResolver getTypeResolver() {
		return null;
	}
}
