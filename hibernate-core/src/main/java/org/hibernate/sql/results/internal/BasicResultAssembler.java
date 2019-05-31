/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.convert.spi.ConvertibleValueMapping;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicResultAssembler<J> implements DomainResultAssembler<J> {
	private final SqmExpressable<J> expressableType;
	private final BasicValueConverter<J,?> valueConverter;

	public BasicResultAssembler(SqmExpressable<J> expressableType) {
		this(
				expressableType,
				expressableType instanceof ConvertibleValueMapping
						? ( (ConvertibleValueMapping<J>) expressableType ).getValueConverter()
						: null
		);
	}

	public BasicResultAssembler(SqmExpressable<J> expressableType, BasicValueConverter<J, ?> valueConverter) {
		this.expressableType = expressableType;
		this.valueConverter = valueConverter;
	}

	@Override
	public Object assemble(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public JavaTypeDescriptor<J> getJavaTypeDescriptor() {
		return expressableType.getExpressableJavaTypeDescriptor();
	}
}
