/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.queryable.spi;

import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface BasicValuedExpressableType<T> extends ExpressableType<T>, SqlSelectable {
	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	BasicJavaDescriptor getJavaTypeDescriptor();

	SqlTypeDescriptor getSqlTypeDescriptor();

	// todo (6.0) : ? - expose AttributeConverter here as method?
	//		atm the expectation is for code check if the ExpressableType is a ConvertibleNavigable.
}
