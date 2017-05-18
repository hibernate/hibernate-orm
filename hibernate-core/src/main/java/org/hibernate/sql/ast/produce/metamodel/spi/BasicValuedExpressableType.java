/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public interface BasicValuedExpressableType<J> extends ConvertibleNavigable<J>, ExpressableType<J> {
	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	BasicJavaDescriptor<J> getJavaTypeDescriptor();

	// todo (6.0) : moved this down to BasicValuedNavigable#getSqlTypeDescriptor
	//		uncomment if we find this is needed as part of being queryable
	//SqlTypeDescriptor getSqlTypeDescriptor();
}
