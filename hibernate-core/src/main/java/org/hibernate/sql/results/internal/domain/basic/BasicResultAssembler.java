/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.basic;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicResultAssembler implements DomainResultAssembler {
	private final SqlSelection sqlSelection;
	private final BasicValueConverter valueConverter;
	private final JavaTypeDescriptor javaTypeDescriptor;

	public BasicResultAssembler(
			SqlSelection sqlSelection,
			BasicValueConverter valueConverter,
			JavaTypeDescriptor javaTypeDescriptor) {
		this.sqlSelection = sqlSelection;
		this.valueConverter = valueConverter;
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object assemble(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		final Object rawJdbcValue = rowProcessingState.getJdbcValue( sqlSelection );

		if ( valueConverter != null ) {
			return valueConverter.toDomainValue( rawJdbcValue, rowProcessingState.getJdbcValuesSourceProcessingState().getSession() );
		}

		return rawJdbcValue;
	}
}
