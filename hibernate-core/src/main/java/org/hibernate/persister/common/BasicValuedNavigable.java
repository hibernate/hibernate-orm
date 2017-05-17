/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common;

import org.hibernate.persister.model.relational.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public interface BasicValuedNavigable<J> extends BasicValuedExpressableType<J>, Navigable<J> {
	Column getBoundColumn();

	// todo (6.0) : dont particularly like this name.
	BasicType<J> getBasicType();

	@Override
	default BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return getBasicType().getJavaTypeDescriptor();
	}

	default SqlTypeDescriptor getSqlTypeDescriptor() {
		return getBasicType().getColumnMapping().getSqlTypeDescriptor();
	}
}
