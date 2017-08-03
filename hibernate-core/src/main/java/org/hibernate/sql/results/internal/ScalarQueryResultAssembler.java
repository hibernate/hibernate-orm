/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.sql.results.spi.ScalarQueryResult;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.QueryResultAssembler;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ScalarQueryResultAssembler implements QueryResultAssembler {
	private final SqlSelection sqlSelection;
	private final ScalarQueryResult returnScalar;

	public ScalarQueryResultAssembler(SqlSelection sqlSelection, ScalarQueryResult returnScalar) {
		this.sqlSelection = sqlSelection;
		this.returnScalar = returnScalar;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return returnScalar.getType().getJavaTypeDescriptor();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object assemble(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		final Object rawJdbcValue = rowProcessingState.getJdbcValue( sqlSelection );

		if ( returnScalar.getType() instanceof ConvertibleNavigable ) {
			final AttributeConverterDefinition attributeConverter = ( (ConvertibleNavigable) returnScalar.getType() ).getAttributeConverter();
			if ( attributeConverter != null ) {
				// apply the attribute converter
				return attributeConverter.getAttributeConverter().convertToEntityAttribute( rawJdbcValue );
			}
		}

		return rawJdbcValue;
	}
}
