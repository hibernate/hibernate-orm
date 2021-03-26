/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectionMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Nathan Xu
 */
public class EntityRowIdMappingImpl implements EntityRowIdMapping, SelectionMapping {
	private final String rowIdName;
	private final EntityMappingType declaringType;
	private final String tableExpression;

	public EntityRowIdMappingImpl(String rowIdName, String tableExpression, EntityMappingType declaringType) {
		this.rowIdName = rowIdName;
		this.tableExpression = tableExpression;
		this.declaringType = declaringType;
	}

	@Override
	public String getRowIdName() {
		return rowIdName;
	}

	@Override
	public MappingType getPartMappingType() {
		return this::getJavaTypeDescriptor;
	}

	@Override
	public JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		return JavaObjectType.INSTANCE.getJavaTypeDescriptor();
	}

	@Override
	public String getPartName() {
		return rowIdName;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return declaringType;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();

		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference columnTableReference = tableGroup.resolveTableReference( tableExpression );

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( columnTableReference, rowIdName ),
						sqlAstProcessingState -> new ColumnReference(
								columnTableReference,
								rowIdName,
								false,
								// todo (6.0) : allowing custom read / write transformers on ROW_ID might
								//		be an easy way to allow customization of the how ROW_ID is rendered
								//		- e.g. quickly testing whether that syntax works for a db without
								//			having to write a Dialect
								null,
								null,
								JavaObjectType.INSTANCE,
								sqlAstCreationState.getCreationContext().getSessionFactory()
						)
				),
				JavaObjectType.INSTANCE.getJdbcMapping().getJavaTypeDescriptor(),
				sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
		);

		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				getJavaTypeDescriptor(),
				navigablePath
		);
	}

	@Override
	public int getJdbcTypeCount() {
		return 1;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, JavaObjectType.INSTANCE );
		return getJdbcTypeCount();
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( domainValue, this );
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public String getSelectionExpression() {
		return rowIdName;
	}

	@Override
	public String getCustomReadExpression() {
		return rowIdName;
	}

	@Override
	public String getCustomWriteExpression() {
		return null;
	}

	@Override
	public boolean isFormula() {
		return true;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return StandardBasicTypes.INTEGER;
	}
}
