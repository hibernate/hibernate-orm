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
package org.hibernate.jpa.internal.metamodel.builder;

import java.lang.reflect.Member;
import javax.persistence.metamodel.Attribute;

import org.hibernate.jpa.internal.metamodel.AbstractManagedType;
import org.hibernate.metamodel.spi.binding.AttributeBinding;

/**
 * Base implementation of AttributeMetadata
 *
 * @author Steve Ebersole
 */
public abstract class BaseAttributeMetadata<X,Y> implements AttributeMetadata<X,Y> {
	private final AttributeBinding attributeBinding;
	private final AbstractManagedType<X> ownerType;
	private final Member member;
	private final Class<Y> javaType;
	private final Attribute.PersistentAttributeType persistentAttributeType;

	@SuppressWarnings({ "unchecked" })
	public BaseAttributeMetadata(
			AttributeBinding attributeBinding,
			AbstractManagedType<X> ownerType,
			Member member,
			Attribute.PersistentAttributeType persistentAttributeType) {
		this.attributeBinding = attributeBinding;
		this.ownerType = ownerType;
		this.member = member;
		this.persistentAttributeType = persistentAttributeType;
		
		this.javaType = (Class<Y>) AttributeBuilder.determineDeclaredType( member );
	}

	@Override
	public String getName() {
		return attributeBinding.getAttribute().getName();
	}

	@Override
	public Member getMember() {
		return member;
	}

	public String getMemberDescription() {
		return determineMemberDescription( getMember() );
	}

	public String determineMemberDescription(Member member) {
		return member.getDeclaringClass().getName() + '#' + member.getName();
	}

	@Override
	public Class<Y> getJavaType() {
		return javaType;
	}

	@Override
	public Attribute.PersistentAttributeType getPersistentAttributeType() {
		return persistentAttributeType;
	}

	@Override
	public AbstractManagedType<X> getOwnerType() {
		return ownerType;
	}

	@Override
	public boolean isPlural() {
		return ! attributeBinding.getAttribute().isSingular();
	}

	@Override
	public AttributeBinding getAttributeBinding() {
		return attributeBinding;
	}
}
