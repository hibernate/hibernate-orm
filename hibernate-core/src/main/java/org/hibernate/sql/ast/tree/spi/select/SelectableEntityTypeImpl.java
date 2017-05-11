/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.persister.common.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceGroup;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceGroupEmptyImpl;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceGroupImpl;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.tree.spi.from.ColumnReference;
import org.hibernate.sql.ast.produce.result.internal.QueryResultEntityImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.consume.results.internal.SqlSelectionGroupImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionGroup;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionGroupEmpty;

/**
 * @author Steve Ebersole
 */
public class SelectableEntityTypeImpl implements Selectable {
	private final Expression expression;
	private final NavigablePath navigablePath;
	private final ColumnReferenceSource columnBindingSource;
	private final EntityPersister<?> entityPersister;

	private final LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> columnBindingGroupMap;
	private final boolean isShallow;

	public SelectableEntityTypeImpl(
			Expression expression,
			NavigablePath navigablePath,
			ColumnReferenceSource columnBindingSource,
			EntityPersister entityPersister,
			boolean isShallow) {
		this.expression = expression;
		this.navigablePath = navigablePath;
		this.columnBindingSource = columnBindingSource;
		this.entityPersister = entityPersister;
		this.columnBindingGroupMap = buildColumnBindingGroupMap( isShallow );
		this.isShallow = isShallow;
	}

	private LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> buildColumnBindingGroupMap(boolean isShallow) {
		final LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> columnBindingGroupMap = new LinkedHashMap<>();

		// no matter what, include:
		//		1) identifier
		addColumnBindingGroupEntry( entityPersister.getHierarchy().getIdentifierDescriptor(), columnBindingGroupMap );
		//		2) ROW_ID (if used)
		if ( entityPersister.getHierarchy().getRowIdDescriptor() != null ) {
			addColumnBindingGroupEntry( entityPersister.getHierarchy().getRowIdDescriptor(), columnBindingGroupMap );
		}
		//		3) discriminator (if used)
		if ( entityPersister.getHierarchy().getDiscriminatorDescriptor() != null ) {
			addColumnBindingGroupEntry( entityPersister.getHierarchy().getDiscriminatorDescriptor(), columnBindingGroupMap );
		}

		// Only render the rest of the attributes if !shallow
		if ( !isShallow ) {
			for ( PersistentAttribute<?,?> persistentAttribute : entityPersister.getPersistentAttributes() ) {
				addColumnBindingGroupEntry( persistentAttribute, columnBindingGroupMap );
			}
		}

		return columnBindingGroupMap;
	}

	private void addColumnBindingGroupEntry(
			PersistentAttribute persistentAttribute,
			Map<PersistentAttribute, ColumnReferenceGroup> columnBindingGroupMap) {
		if ( !SingularPersistentAttribute.class.isInstance( persistentAttribute ) ) {
			columnBindingGroupMap.put( persistentAttribute, ColumnReferenceGroupEmptyImpl.INSTANCE );
			return;
		}

		final SingularPersistentAttribute singularAttribute = (SingularPersistentAttribute) persistentAttribute;
		final ColumnReferenceGroupImpl columnBindingGroup = new ColumnReferenceGroupImpl();

		final List<Column> columns;
		if ( persistentAttribute instanceof SingularPersistentAttributeEmbedded ) {
			columns = ( (SingularPersistentAttributeEmbedded) singularAttribute ).getEmbeddablePersister().collectColumns();
		}
		else {
			columns = singularAttribute.getColumns();
		}

		for ( Column column : columns ) {
			columnBindingGroup.addColumnBinding( columnBindingSource.resolveColumnReference( column ) );
		}

		columnBindingGroupMap.put( persistentAttribute, columnBindingGroup );
	}

	@Override
	public Expression getSelectedExpression() {
		return expression;
	}

	@Override
	public QueryResult toQueryReturn(QueryResultCreationContext returnResolutionContext, String resultVariable) {
		return new QueryResultEntityImpl(
				expression,
				entityPersister,
				resultVariable,
				isShallow,
				buildSqlSelectionGroupMap( returnResolutionContext ),
				navigablePath,
				columnBindingSource.getTableGroup().getUid()
		);
	}

	private Map<PersistentAttribute, SqlSelectionGroup> buildSqlSelectionGroupMap(QueryResultCreationContext resolutionContext) {
		final Map<PersistentAttribute, SqlSelectionGroup> sqlSelectionGroupMap = new HashMap<>();

		for ( Map.Entry<PersistentAttribute, ColumnReferenceGroup> entry : columnBindingGroupMap.entrySet() ) {
			sqlSelectionGroupMap.put(
					entry.getKey(),
					toSqlSelectionGroup( entry.getValue(), resolutionContext )
			);
		}

		return sqlSelectionGroupMap;
	}

	private SqlSelectionGroup toSqlSelectionGroup(ColumnReferenceGroup columnBindingGroup, QueryResultCreationContext resolutionContext) {
		if ( columnBindingGroup.getColumnReferences().isEmpty() ) {
			return SqlSelectionGroupEmpty.INSTANCE;
		}

		final SqlSelectionGroupImpl sqlSelectionGroup = new SqlSelectionGroupImpl();
		for ( ColumnReference columnBinding : columnBindingGroup.getColumnReferences() ) {
			sqlSelectionGroup.addSqlSelection( resolutionContext.resolveSqlSelection( columnBinding ) );
		}
		return sqlSelectionGroup;
	}

	public List<ColumnReference> getColumnBinding() {
		List<ColumnReference> columnBindings = null;

		for ( ColumnReferenceGroup columnBindingGroup : columnBindingGroupMap.values() ) {
			if ( columnBindingGroup.getColumnReferences().isEmpty() ) {
				continue;
			}

			if ( columnBindings == null ) {
				columnBindings = new ArrayList<>();
			}
			columnBindings.addAll( columnBindingGroup.getColumnReferences() );
		}

		return columnBindings == null ? Collections.emptyList() : columnBindings;
	}
}
