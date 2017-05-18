/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.metamodel.queryable.spi.BasicValuedExpressableType;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.produce.result.internal.QueryResultScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.internal.NavigableSelection;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class VersionDescriptorImpl<O,J>
		extends AbstractSingularPersistentAttribute<O,J,BasicType<J>>
		implements VersionDescriptor<O,J>, BasicValuedExpressableType<J> {
	private final Column column;
	private final String unsavedValue;

	public VersionDescriptorImpl(
			EntityHierarchy hierarchy,
			RootClass rootEntityBinding,
			Column column,
			String name,
			BasicType<J> ormType,
			boolean nullable,
			String unsavedValue,
			RuntimeModelCreationContext creationContext) {
		super(
				hierarchy.getRootEntityType(),
				name,
				PersisterHelper.resolvePropertyAccess( hierarchy.getRootEntityType(), rootEntityBinding.getVersion(), creationContext ),
				ormType,
				Disposition.NORMAL,
				nullable
		);
		this.column = column;
		this.unsavedValue = unsavedValue;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public String getUnsavedValue() {
		return unsavedValue;
	}


	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( column );
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return getContainer().asLoggableText() + '.' + getNavigableName();
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitVersion( this );
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return (BasicJavaDescriptor) super.getJavaTypeDescriptor();
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return getOrmType().getColumnMappings()[0].getSqlTypeDescriptor();
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		assert selectedExpression instanceof NavigableReference;
		return new NavigableSelection( (NavigableReference) selectedExpression, resultVariable );
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
				sqlSelectionResolver.resolveSqlSelection( columnReferenceSource.resolveColumnReference( column ) ),
				resultVariable,
				this
		);
	}
}
