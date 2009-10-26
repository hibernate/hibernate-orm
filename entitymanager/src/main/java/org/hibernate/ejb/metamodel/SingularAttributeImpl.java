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
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class SingularAttributeImpl<X, Y>
		extends AbstractAttribute<X,Y>
		implements SingularAttribute<X, Y>, Serializable {
	private final boolean isIdentifier;
	private final boolean isVersion;
	private final boolean isOptional;
	private final Type<Y> attributeType;

	public SingularAttributeImpl(
			String name,
			Class<Y> javaType,
			AbstractManagedType<X> declaringType,
			Member member,
			boolean isIdentifier,
			boolean isVersion,
			boolean isOptional,
			Type<Y> attributeType,
			PersistentAttributeType persistentAttributeType) {
		super( name, javaType, declaringType, member, persistentAttributeType );
		this.isIdentifier = isIdentifier;
		this.isVersion = isVersion;
		this.isOptional = isOptional;
		this.attributeType = attributeType;
	}

	/**
	 * Subclass used to simply instantiation of singular attributes representing an entity's
	 * identifier.
	 */
	public static class Identifier<X,Y> extends SingularAttributeImpl<X,Y> {
		public Identifier(
				String name,
				Class<Y> javaType,
				AbstractManagedType<X> declaringType,
				Member member,
				Type<Y> attributeType,
				PersistentAttributeType persistentAttributeType) {
			super( name, javaType, declaringType, member, true, false, false, attributeType, persistentAttributeType );
		}
	}

	/**
	 * Subclass used to simply instantiation of singular attributes representing an entity's
	 * version.
	 */
	public static class Version<X,Y> extends SingularAttributeImpl<X,Y> {
		public Version(
				String name,
				Class<Y> javaType,
				AbstractManagedType<X> declaringType,
				Member member,
				Type<Y> attributeType,
				PersistentAttributeType persistentAttributeType) {
			super( name, javaType, declaringType, member, false, true, false, attributeType, persistentAttributeType );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isId() {
		return isIdentifier;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isVersion() {
		return isVersion;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isOptional() {
		return isOptional;
	}

	/**
	 * {@inheritDoc}
	 */
	public Type<Y> getType() {
		return attributeType;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isAssociation() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isCollection() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public BindableType getBindableType() {
		return BindableType.SINGULAR_ATTRIBUTE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class<Y> getBindableJavaType() {
		return attributeType.getJavaType();
	}
}
