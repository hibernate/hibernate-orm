/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.metamodel;

import java.lang.reflect.Member;
import java.io.Serializable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.mapping.Property;

/**
 * @author Emmanuel Bernard
 */
public class SingularAttributeImpl<X, Y> implements SingularAttribute<X, Y>, Serializable {
	private final boolean isId;
	private final boolean isVersion;
	private final boolean isOptional;
	private final ManagedType<X> ownerType;
	private final Type<Y> attrType;
	//FIXME member is not serializable
	private final Member member;
	private final String name;
	private final PersistentAttributeType persistentAttributeType;

	private SingularAttributeImpl(Builder<X,Y> builder) {
		this.ownerType = builder.type;
		this.attrType = builder.attributeType;
		this.isId = builder.isId;
		this.isVersion = builder.isVersion;
		final Property property = builder.property;
		this.isOptional = property.isOptional();
		this.member = builder.member;
		this.name = property.getName();
		if ( builder.persistentAttributeType != null) {
			this.persistentAttributeType = builder.persistentAttributeType;
		}
		else {
			this.persistentAttributeType = property.isComposite() ?
														PersistentAttributeType.EMBEDDED :
														PersistentAttributeType.BASIC;
		}
	}

	public static class Builder<X,Y> {
		private boolean isId;
		private boolean isVersion;
		//private boolean isOptional = true;
		private final Type<Y> attributeType;
		private final ManagedType<X> type;
		private Member member;
		//private String name;
		private PersistentAttributeType persistentAttributeType;
		private Property property;


		private Builder(ManagedType<X> ownerType, Type<Y> attrType) {
			this.type = ownerType;
			this.attributeType = attrType;
		}

		public Builder<X,Y> member(Member member) {
			this.member = member;
			return this;
		}

		public Builder<X, Y> property(Property property) {
			this.property = property;
			return this;
		}

		public Builder<X,Y> id() {
			isId = true;
			return this;
		}

		public Builder<X,Y> version() {
			isVersion = true;
			return this;
		}

		public SingularAttribute<X, Y> build() {
			return new SingularAttributeImpl<X,Y>(this);
		}

		public Builder<X, Y> persistentAttributeType(PersistentAttributeType attrType) {
			this.persistentAttributeType = attrType;
			return this;
		}
	}

	public static <X,Y> Builder<X,Y> create(ManagedType<X> ownerType, Type<Y> attrType) {
		return new Builder<X,Y>(ownerType, attrType);
	}

	public boolean isId() {
		return isId;
	}

	public boolean isVersion() {
		return isVersion;
	}

	public boolean isOptional() {
		return isOptional;
	}

	public Type<Y> getType() {
		return attrType;
	}

	public String getName() {
		return name;
	}

	public PersistentAttributeType getPersistentAttributeType() {
		return persistentAttributeType;
	}

	public ManagedType<X> getDeclaringType() {
		return ownerType;
	}

	public Class<Y> getJavaType() {
		return attrType.getJavaType();
	}

	public Member getJavaMember() {
		return member;
	}

	public boolean isAssociation() {
		return false;
	}

	public boolean isCollection() {
		return false;
	}

	public BindableType getBindableType() {
		return BindableType.SINGULAR_ATTRIBUTE;
	}

	public Class<Y> getBindableJavaType() {
		return attrType.getJavaType();
	}
}
