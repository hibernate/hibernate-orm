/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.results.internal;

import java.sql.SQLException;

import org.hibernate.persister.common.spi.ConvertibleNavigable;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.ast.produce.result.spi.QueryResultScalar;
import org.hibernate.sql.ast.consume.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.ast.consume.results.spi.RowProcessingState;
import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;

/**
 * @author Steve Ebersole
 */
public class QueryResultAssemblerScalar implements QueryResultAssembler {
	private final SqlSelection sqlSelection;
	private final QueryResultScalar returnScalar;

	public QueryResultAssemblerScalar(SqlSelection sqlSelection, QueryResultScalar returnScalar) {
		this.sqlSelection = sqlSelection;
		this.returnScalar = returnScalar;
	}

	@Override
	public Class getReturnedJavaType() {
		// todo (6.0) : remove the ReturnAssembler#getReturnedJavaType method.
		//		It is only used for resolving dynamic-instantiation arguments which should
		//		not be modeled as Returns anyway...
		return returnScalar.getReturnedJavaType();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object assemble(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) throws SQLException {
		final Object rawJdbcValue = rowProcessingState.getJdbcValues()[sqlSelection.getValuesArrayPosition()];

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
