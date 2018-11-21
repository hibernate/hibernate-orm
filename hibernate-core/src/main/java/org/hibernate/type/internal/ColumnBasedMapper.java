/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.MutableMutabilityPlan;

/**
 * @author Steve Ebersole
 */
public class ColumnBasedMapper<J> implements BasicValueMapper<J> {
	private final Column column;

	public ColumnBasedMapper(Column column) {
		this.column = column;
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicJavaDescriptor<J> getDomainJavaDescriptor() {
		return column.getJavaTypeDescriptor();
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return column.getExpressableType();
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return null;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return column.getJavaTypeDescriptor().getMutabilityPlan();
	}
}
