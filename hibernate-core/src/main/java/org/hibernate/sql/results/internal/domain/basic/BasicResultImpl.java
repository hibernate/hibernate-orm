/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.basic;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.ScalarResult;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicResultImpl implements ScalarResult {
	private final String resultVariable;
	private final SqlExpressableType expressableType;

	private final DomainResultAssembler assembler;

	public BasicResultImpl(
			String resultVariable,
			SqlSelection sqlSelection,
			SqlExpressableType expressableType) {
		this.resultVariable = resultVariable;
		this.expressableType = expressableType;

		// todo (6.0) : consider using `org.hibernate.metamodel.model.domain.spi.BasicValueConverter` instead
		//		I'd like to get rid of exposing the AttributeConverter from the model

		/// todo (6.0) : actually, conversions ought to occur as part of
		BasicValueConverter valueConverter = null;
		if ( expressableType instanceof ConvertibleNavigable ) {
			valueConverter = ( (ConvertibleNavigable) expressableType ).getValueConverter();
		}

		this.assembler = new BasicResultAssembler( sqlSelection, valueConverter, expressableType.getJavaTypeDescriptor() );
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return expressableType.getJavaTypeDescriptor();
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationOptions,
			AssemblerCreationContext creationContext) {
		return assembler;
	}
}
