/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import javax.persistence.AttributeConverter;

import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.results.spi.InitializerCollector;
import org.hibernate.sql.results.spi.QueryResultAssembler;
import org.hibernate.sql.results.spi.ScalarQueryResult;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class ScalarQueryResultImpl implements ScalarQueryResult {
	private final String resultVariable;
	private final BasicValuedExpressableType expressableType;

	private final QueryResultAssembler assembler;

	public ScalarQueryResultImpl(
			String resultVariable,
			SqlSelection sqlSelection,
			BasicValuedExpressableType expressableType) {
		this.resultVariable = resultVariable;
		this.expressableType = expressableType;

		// todo (6.0) : consider using `org.hibernate.metamodel.model.domain.spi.BasicValueConverter` instead
		//		I'd like to get rid of exposing the AttributeConverter from the model
		AttributeConverter attributeConverter = null;
		if ( expressableType instanceof ConvertibleNavigable ) {
			final ConvertibleNavigable navigable = (ConvertibleNavigable) expressableType;
			if ( navigable.getAttributeConverterDefinition() != null ) {
				attributeConverter = navigable.getAttributeConverterDefinition().getAttributeConverter();
			}
		}

		this.assembler = new ScalarQueryResultAssembler( sqlSelection, attributeConverter, expressableType.getJavaTypeDescriptor() );
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
	public void registerInitializers(InitializerCollector collector) {
		// nothing to do
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
