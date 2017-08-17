/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import javax.persistence.AttributeConverter;

import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.QueryResultAssembler;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ScalarQueryResultAssembler implements QueryResultAssembler {
	private final SqlSelection sqlSelection;
	private final AttributeConverter attributeConverter;
	private final JavaTypeDescriptor javaTypeDescriptor;

	public ScalarQueryResultAssembler(
			SqlSelection sqlSelection,
			AttributeConverter attributeConverter,
			JavaTypeDescriptor javaTypeDescriptor) {
		this.sqlSelection = sqlSelection;
		this.attributeConverter = attributeConverter;
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

		if ( attributeConverter != null ) {
			return attributeConverter.convertToEntityAttribute( rawJdbcValue );
		}

		return rawJdbcValue;
	}
}
