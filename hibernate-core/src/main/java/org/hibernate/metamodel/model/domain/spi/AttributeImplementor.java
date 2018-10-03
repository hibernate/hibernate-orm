/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.Attribute;

/**
 * Hibernate extension to the JPA {@link Attribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface AttributeImplementor<D, J> extends Attribute<D, J> {
	@Override
	ManagedTypeImplementor<D> getDeclaringType();

	SimpleTypeImplementor<?> getValueGraphType();
	SimpleTypeImplementor<?> getKeyGraphType();
}
