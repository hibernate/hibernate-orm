/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.io.Serializable;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.type.ComponentType;

/**
 * @author Emmanuel Bernard
 */
public class EmbeddableTypeImpl<X>
		extends AbstractManagedType<X>
		implements EmbeddableType<X>, Serializable {

	private final AbstractManagedType parent;
	private final String attributeName;
	private final ComponentType hibernateType;

	public EmbeddableTypeImpl(Class<X> javaType, AbstractManagedType parent,String attributeName, ComponentType hibernateType) {
		super( javaType, null, null );
		this.parent = parent;
		this.attributeName = attributeName;
		this.hibernateType = hibernateType;
	}

	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	public AbstractManagedType getParent() {
		return parent;
	}

	public ComponentType getHibernateType() {
		return hibernateType;
	}

	public String resolveRole() {
		if ( EmbeddableTypeImpl.class.isInstance( getParent() ) ) {
			final EmbeddableTypeImpl embeddableParent = (EmbeddableTypeImpl) parent;
			return embeddableParent.resolveRole() + '.' + attributeName;
		}
		return parent.getTypeName() + '.' + attributeName;
	}
}
