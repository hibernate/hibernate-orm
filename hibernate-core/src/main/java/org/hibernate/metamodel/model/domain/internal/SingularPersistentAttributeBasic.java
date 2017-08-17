/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableBasicValued;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressionQualifier;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeBasic<O,J>
		extends AbstractSingularPersistentAttribute<O, J>
		implements NavigableBasicValued<J>, ConvertibleNavigable<J> {

	private final Column boundColumn;
	private final BasicType<J> basicType;
	private final AttributeConverterDefinition converterDefinition;

	public SingularPersistentAttributeBasic(
			ManagedTypeDescriptor<O> declaringType,
			String name,
			PropertyAccess propertyAccess,
			Disposition disposition,
			boolean nullable,
			BasicValueMapping<J> basicValueMapping,
			RuntimeModelCreationContext context) {
		super( declaringType, name, propertyAccess, disposition, nullable, basicValueMapping );

		this.boundColumn = context.getDatabaseObjectResolver().resolveColumn( basicValueMapping.getMappedColumn() );
		this.basicType = basicValueMapping.resolveType();
		this.converterDefinition = basicValueMapping.getAttributeConverterDefinition();
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
	public QueryResult createQueryResult(
			Expression expression,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return new ScalarQueryResultImpl(
				resultVariable,
				creationContext.getSqlSelectionResolver().resolveSqlSelection(
						creationContext.getSqlSelectionResolver().resolveSqlExpression(
								creationContext.currentColumnReferenceSource(),
								boundColumn
						)
				),
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
		return converterDefinition;
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

	@Override
	public ValueBinder getValueBinder() {
		return basicType.getValueBinder();
	}

	@Override
	public ValueExtractor getValueExtractor() {
		return basicType.getValueExtractor();
	}

	@Override
	public SqlSelectionGroup resolveSqlSelectionGroup(
			SqlExpressionQualifier qualifier,
			SqlSelectionGroupResolutionContext resolutionContext) {
		throw new NotYetImplementedException(  );
	}
}
