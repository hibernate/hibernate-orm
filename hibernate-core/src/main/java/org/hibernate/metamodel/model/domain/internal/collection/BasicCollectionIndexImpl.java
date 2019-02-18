/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionIndex;
import org.hibernate.metamodel.model.domain.spi.BasicCollectionIndex;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class BasicCollectionIndexImpl<J>
		extends AbstractCollectionIndex<J>
		implements BasicCollectionIndex<J>, ConvertibleNavigable<J> {
	private static final Logger log = Logger.getLogger( BasicCollectionIndexImpl.class );

	private final Column column;
	private final BasicValueMapper<J> valueMapper;
	private final boolean nullable;

	@SuppressWarnings("unchecked")
	public BasicCollectionIndexImpl(
			PersistentCollectionDescriptor descriptor,
			IndexedCollection bootCollectionMapping,
			RuntimeModelCreationContext creationContext) {
		super( descriptor, bootCollectionMapping );

		final BasicValueMapping valueMapping = (BasicValueMapping) bootCollectionMapping.getIndex();
		MappedColumn mappedColumn = valueMapping.getMappedColumn();
		Column resolvedColumn = creationContext.getDatabaseObjectResolver().resolveColumn( mappedColumn );

		if ( resolvedColumn != null ) {
			this.column = resolvedColumn.clone( mappedColumn.isInsertable(), mappedColumn.isUpdatable() );
		}
		else {
			this.column = null;
		}

		this.valueMapper = valueMapping.getResolution().getValueMapper();

		if ( valueMapper.getValueConverter() != null ) {
			log.debugf(
					"BasicValueConverter [%s] being applied for basic collection elements : %s",
					valueMapper.getValueConverter(),
					getNavigableRole()
			);
		}

		this.nullable = bootCollectionMapping.getIndex().isNullable();
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return valueMapper.getDomainJavaDescriptor();
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueMapper.getValueConverter();
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return valueMapper;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return valueMapper.getSqlExpressableType();
	}

	@Override
	public SimpleTypeDescriptor<?> getDomainTypeDescriptor() {
		return this;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( getBoundColumn() );
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmCollectionIndexReferenceBasic( (SqmPluralAttributeReference) containerReference );
	}

	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationState creationState, DomainResultCreationContext creationContext) {
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						creationState.getSqlExpressionResolver().resolveSqlExpression(
								navigableReference.getColumnReferenceQualifier(),
								getBoundColumn()
						),
						getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getBoundColumn().getExpressableType()
		);
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		// nothing to do
	}

	@Override
	public boolean hasNotNullColumns() {
		return !nullable;
	}
}
