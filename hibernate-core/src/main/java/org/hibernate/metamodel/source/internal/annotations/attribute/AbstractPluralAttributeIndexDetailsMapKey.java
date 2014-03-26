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
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolverComposition;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.EnumeratedTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.HibernateTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.TemporalTypeResolver;


/**
 * PluralAttributeIndexDetails implementation for describing the key of a Map
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPluralAttributeIndexDetailsMapKey implements PluralAttributeIndexDetails {
	private final PluralAttribute pluralAttribute;
	private final JavaTypeDescriptor resolvedMapKeyType;
	private final AttributeTypeResolver typeResolver;

	public AbstractPluralAttributeIndexDetailsMapKey(
			PluralAttribute pluralAttribute,
			MemberDescriptor backingMember,
			JavaTypeDescriptor resolvedMapKeyType) {
		this.pluralAttribute = pluralAttribute;
		this.resolvedMapKeyType = resolvedMapKeyType;

		this.typeResolver = new AttributeTypeResolverComposition(
				resolvedMapKeyType,
				pluralAttribute.getContext(),
				HibernateTypeResolver.createCollectionIndexTypeResolver( pluralAttribute, resolvedMapKeyType ),
				EnumeratedTypeResolver.createCollectionIndexTypeResolver( pluralAttribute, resolvedMapKeyType ),
				TemporalTypeResolver.createCollectionIndexTypeResolver( pluralAttribute, resolvedMapKeyType )
		);
	}

	@Override
	public JavaTypeDescriptor getJavaType() {
		return resolvedMapKeyType;
	}

	@Override
	public AttributeTypeResolver getTypeResolver() {
		return typeResolver;
	}
}
