/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.criteria.path;

import java.io.Serializable;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.PathSource;

/**
 * Models a path for a {@link SingularAttribute} generally obtained from a
 * {@link javax.persistence.criteria.Path#get(SingularAttribute)} call
 *
 * @author Steve Ebersole
 */
public class SingularAttributePath<X> extends AbstractPathImpl<X> implements Serializable {
	private final SingularAttribute<?,X> attribute;
	private final ManagedType<X> managedType;

	@SuppressWarnings({ "unchecked" })
	public SingularAttributePath(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			PathSource pathSource,
			SingularAttribute<?, X> attribute) {
		super( criteriaBuilder, javaType, pathSource );
		this.attribute = attribute;
		this.managedType = resolveManagedType( attribute );
	}

	private ManagedType<X> resolveManagedType(SingularAttribute<?, X> attribute) {
		if ( Attribute.PersistentAttributeType.BASIC == attribute.getPersistentAttributeType() ) {
			return null;
		}
		else if ( Attribute.PersistentAttributeType.EMBEDDED == attribute.getPersistentAttributeType() ) {
			return (EmbeddableType<X>) attribute.getType();
		}
		else {
			return (IdentifiableType<X>) attribute.getType();
//			return criteriaBuilder.getEntityManagerFactory()
//					.getMetamodel()
//					.managedType( javaType );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public SingularAttribute<?, X> getAttribute() {
		return attribute;
	}

	/**
	 * {@inheritDoc}
	 */
	public Bindable<X> getModel() {
		return getAttribute();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean canBeDereferenced() {
		return managedType != null;
	}

	@Override
	protected Attribute locateAttributeInternal(String attributeName) {
		final Attribute attribute = managedType.getAttribute( attributeName );
		// ManagedType.getAttribute should throw exception rather than return
		// null, but just to be safe...
		if ( attribute == null ) {
			throw new IllegalArgumentException( "Could not resolve attribute named " + attributeName );
		}
		return attribute;
	}
}
