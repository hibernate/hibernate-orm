/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public interface BasicValuedExpressableType<J>
		extends ExpressableType<J>, AllowableParameterType<J>, AllowableFunctionReturnType<J> {
	BasicType<J> getBasicType();

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	BasicJavaDescriptor<J> getJavaTypeDescriptor();

	@Override
	default int getNumberOfJdbcParametersToBind() {
		return 1;
	}

	// todo (6.0) : moved this down to BasicValuedNavigable#getSqlTypeDescriptor
	//		uncomment if we find this is needed as part of being queryable
	//SqlTypeDescriptor getSqlTypeDescriptor();
}
