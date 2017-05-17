/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.persister.common.BasicValuedNavigable;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.model.relational.spi.Column;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.produce.result.internal.QueryResultScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeBasic<O,J>
		extends AbstractSingularPersistentAttribute<O, J>
		implements BasicValuedNavigable<J> {

	private final Column boundColumn;
	private final BasicType<J> basicType;
	private AttributeConverterDefinition attributeConverterInfo;

	public SingularPersistentAttributeBasic(
			ManagedTypeImplementor<O> declaringType,
			String name,
			PropertyAccess propertyAccess,
			BasicValuedExpressableType<J> expressableType,
			Disposition disposition,
			AttributeConverterDefinition attributeConverterInfo,
			List<Column> columns) {
		super( declaringType, name, propertyAccess, expressableType, disposition, true );

		assert columns.size() == 1;

		this.attributeConverterInfo = attributeConverterInfo;
		this.boundColumn = columns.get( 0 );

		// todo (6.0) : resolve SimpleValue -> BasicType
		this.basicType = null;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public BasicValuedExpressableType<J> getType() {
		return (BasicValuedExpressableType<J>) super.getType();
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return (BasicJavaDescriptor<J>) super.getJavaTypeDescriptor();
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultScalarImpl(
				selectedExpression,
				sqlSelectionResolver.resolveSqlSelection(
						columnReferenceSource.resolveColumnReference( boundColumn )
				),
				resultVariable,
				getType()
	  	);
	}

	@Override
	public Column getBoundColumn() {
		return boundColumn;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( getBoundColumn() );
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeBasic(" + getContainer().asLoggableText() + '.' + getAttributeName() + ')';
	}

	@Override
	public AttributeConverterDefinition getAttributeConverter() {
		return attributeConverterInfo;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeBasic( this );
	}

	@Override
	public BasicType<J> getBasicType() {
		return basicType;
	}
}
