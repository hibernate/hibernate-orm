/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.metamodel.model.domain.EmbeddedDomainType;
import org.hibernate.type.ComponentType;

/**
 * Hibernate extension to the JPA {@link EmbeddableType} descriptor
 *
 * @author Steve Ebersole
 */
public interface EmbeddedTypeDescriptor<J> extends EmbeddedDomainType<J>, ManagedTypeDescriptor<J> {
	ComponentType getHibernateType();

	ManagedTypeDescriptor<?> getParent();
}
