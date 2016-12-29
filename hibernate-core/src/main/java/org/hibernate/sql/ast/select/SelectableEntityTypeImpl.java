/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.common.internal.SingularAttributeEmbedded;
import org.hibernate.persister.common.spi.Attribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.SingularAttribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.expression.domain.ColumnBindingGroup;
import org.hibernate.sql.ast.expression.domain.ColumnBindingGroupEmptyImpl;
import org.hibernate.sql.ast.expression.domain.ColumnBindingGroupImpl;
import org.hibernate.sql.ast.expression.domain.ColumnBindingSource;
import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.convert.results.internal.ReturnEntityImpl;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sql.exec.results.process.internal.SqlSelectionGroupImpl;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionGroup;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionGroupEmpty;

/**
 * @author Steve Ebersole
 */
public class SelectableEntityTypeImpl implements Selectable {
	private final Expression expression;
	private final PropertyPath propertyPath;
	private final ColumnBindingSource columnBindingSource;
	private final EntityPersister entityPersister;

	private final LinkedHashMap<Attribute, ColumnBindingGroup> columnBindingGroupMap;
	private final boolean isShallow;

	public SelectableEntityTypeImpl(
			Expression expression,
			PropertyPath propertyPath,
			ColumnBindingSource columnBindingSource,
			EntityPersister entityPersister,
			boolean isShallow) {
		this.expression = expression;
		this.propertyPath = propertyPath;
		this.columnBindingSource = columnBindingSource;
		this.entityPersister = entityPersister;
		this.columnBindingGroupMap = buildColumnBindingGroupMap( isShallow );
		this.isShallow = isShallow;
	}

	private LinkedHashMap<Attribute, ColumnBindingGroup> buildColumnBindingGroupMap(boolean isShallow) {
		final LinkedHashMap<Attribute, ColumnBindingGroup> columnBindingGroupMap = new LinkedHashMap<>();

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
			for ( Attribute attribute : entityPersister.getNonIdentifierAttributes() ) {
				addColumnBindingGroupEntry( attribute, columnBindingGroupMap );
			}
		}

		return columnBindingGroupMap;
	}

	private void addColumnBindingGroupEntry(
			Attribute attribute,
			Map<Attribute, ColumnBindingGroup> columnBindingGroupMap) {
		if ( !SingularAttribute.class.isInstance( attribute ) ) {
			columnBindingGroupMap.put( attribute, ColumnBindingGroupEmptyImpl.INSTANCE );
			return;
		}

		final SingularAttribute singularAttribute = (SingularAttribute) attribute;
		final ColumnBindingGroupImpl columnBindingGroup = new ColumnBindingGroupImpl();

		final List<Column> columns;
		if ( attribute instanceof SingularAttributeEmbedded ) {
			columns = ( (SingularAttributeEmbedded) singularAttribute ).getEmbeddablePersister().collectColumns();
		}
		else {
			columns = singularAttribute.getColumns();
		}

		for ( Column column : columns ) {
			columnBindingGroup.addColumnBinding( columnBindingSource.resolveColumnBinding( column ) );
		}

		columnBindingGroupMap.put( attribute, columnBindingGroup );
	}

	@Override
	public Expression getSelectedExpression() {
		return expression;
	}

	@Override
	public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
		return new ReturnEntityImpl(
				expression,
				entityPersister,
				resultVariable,
				isShallow,
				buildSqlSelectionGroupMap( returnResolutionContext ),
				propertyPath,
				columnBindingSource.getTableGroup().getUid()
		);
	}

	private Map<Attribute, SqlSelectionGroup> buildSqlSelectionGroupMap(ReturnResolutionContext resolutionContext) {
		final Map<Attribute, SqlSelectionGroup> sqlSelectionGroupMap = new HashMap<>();

		for ( Map.Entry<Attribute, ColumnBindingGroup> entry : columnBindingGroupMap.entrySet() ) {
			sqlSelectionGroupMap.put(
					entry.getKey(),
					toSqlSelectionGroup( entry.getValue(), resolutionContext )
			);
		}

		return sqlSelectionGroupMap;
	}

	private SqlSelectionGroup toSqlSelectionGroup(ColumnBindingGroup columnBindingGroup, ReturnResolutionContext resolutionContext) {
		if ( columnBindingGroup.getColumnBindings().isEmpty() ) {
			return SqlSelectionGroupEmpty.INSTANCE;
		}

		final SqlSelectionGroupImpl sqlSelectionGroup = new SqlSelectionGroupImpl();
		for ( ColumnBinding columnBinding : columnBindingGroup.getColumnBindings() ) {
			sqlSelectionGroup.addSqlSelection( resolutionContext.resolveSqlSelection( columnBinding ) );
		}
		return sqlSelectionGroup;
	}

	public List<ColumnBinding> getColumnBinding() {
		List<ColumnBinding> columnBindings = null;

		for ( ColumnBindingGroup columnBindingGroup : columnBindingGroupMap.values() ) {
			if ( columnBindingGroup.getColumnBindings().isEmpty() ) {
				continue;
			}

			if ( columnBindings == null ) {
				columnBindings = new ArrayList<>();
			}
			columnBindings.addAll( columnBindingGroup.getColumnBindings() );
		}

		return columnBindings == null ? Collections.emptyList() : columnBindings;
	}
}
