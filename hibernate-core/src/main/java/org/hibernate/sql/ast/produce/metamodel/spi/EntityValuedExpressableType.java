/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * Common contract for any Navigable whose type is an entity.
 *
 * @author Steve Ebersole
 */
public interface EntityValuedExpressableType<T> extends ExpressableType<T>, NavigableContainer<T> {

	// todo (6.0) : should this extend NavigableSource rather than just Navigable?
	// 		or should this just specialize ExpressableType?

	EntityTypeDescriptor<T> getEntityDescriptor();

	String getEntityName();
	String getJpaEntityName();

	default PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	default EntityJavaDescriptor<T> getJavaTypeDescriptor() {
		return getEntityDescriptor().getJavaTypeDescriptor();
	}

}
