/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.basic;

import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A {@link BasicResultAssembler} which does type coercion to handle cases
 * where the expression type and the expected result {@link JavaType} are different
 * (e.g. same column mapped with differently typed properties).
 *
 * @author Marco Belladelli
 */
public class CoercingResultAssembler<J> extends BasicResultAssembler<J> {
	public CoercingResultAssembler(
			int valuesArrayPosition,
			JavaType<J> assembledJavaType,
			BasicValueConverter<J, ?> valueConverter,
			boolean nestedInAggregateComponent) {
		super( valuesArrayPosition, assembledJavaType, valueConverter, nestedInAggregateComponent );
	}

	/**
	 * Access to the row value, coerced to expected type
	 */
	@Override
	public Object extractRawValue(RowProcessingState rowProcessingState) {
		return assembledJavaType.coerce( super.extractRawValue( rowProcessingState ) );
	}
}
