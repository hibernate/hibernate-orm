/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.tree.internal.NavigableSelection;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceGroup;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceGroupEmptyImpl;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceGroupImpl;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * @author Steve Ebersole
 */
public class EntityValuedSelectable implements Selectable {
	private final NavigableReference navigableReference;
	private final NavigablePath navigablePath;
	private final ColumnReferenceSource columnBindingSource;
	private final EntityTypeImplementor<?> entityDescriptor;
	private final boolean isShallow;

	private LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> columnReferenceGroupMap;

	public EntityValuedSelectable(
			NavigableReference navigableReference,
			NavigablePath navigablePath,
			ColumnReferenceSource columnBindingSource,
			boolean isShallow) {
		this.navigableReference = navigableReference;
		this.navigablePath = navigablePath;
		this.columnBindingSource = columnBindingSource;
		this.isShallow = isShallow;

		assert navigableReference.getNavigable() instanceof EntityValuedNavigable;

		final EntityValuedNavigable navigable = (EntityValuedNavigable) navigableReference.getNavigable();
		this.entityDescriptor = navigable.getEntityDescriptor();
	}

	public LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> getColumnReferenceGroupMap() {
		if ( columnReferenceGroupMap == null ) {
			entityDescriptor.createSelection(

			)
			columnReferenceGroupMap = buildColumnBindingGroupMap();
		}
		return columnReferenceGroupMap;
	}

	private LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> buildColumnBindingGroupMap() {
		final LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> columnBindingGroupMap = new LinkedHashMap<>();

		// no matter what, include:
		//		1) identifier
		addColumnBindingGroupEntry( entityDescriptor.getHierarchy().getIdentifierDescriptor(), columnBindingGroupMap );
		//		2) ROW_ID (if used)
		if ( entityDescriptor.getHierarchy().getRowIdDescriptor() != null ) {
			addColumnBindingGroupEntry( entityDescriptor.getHierarchy().getRowIdDescriptor(), columnBindingGroupMap );
		}
		//		3) discriminator (if used)
		if ( entityDescriptor.getHierarchy().getDiscriminatorDescriptor() != null ) {
			addColumnBindingGroupEntry( entityDescriptor.getHierarchy().getDiscriminatorDescriptor(), columnBindingGroupMap );
		}

		// Only render the rest of the attributes if !shallow
		if ( !isShallow ) {
			for ( PersistentAttribute<?,?> persistentAttribute : entityDescriptor.getPersistentAttributes() ) {
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
			columns = ( (SingularPersistentAttributeEmbedded) singularAttribute ).getEmbeddedDescriptor().collectColumns();
		}
		else {
			columns = singularAttribute.getColumns();
		}

		for ( Column column : columns ) {
			columnBindingGroup.addColumnBinding( columnBindingSource.resolveColumnReference( column ) );
		}

		columnBindingGroupMap.put( persistentAttribute, columnBindingGroup );
	}


	public List<ColumnReference> getColumnBinding() {
		List<ColumnReference> columnBindings = null;

		for ( ColumnReferenceGroup columnBindingGroup : columnReferenceGroupMap.values() ) {
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

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		assert selectedExpression instanceof NavigableReference;
		final NavigableReference navigableReference = (NavigableReference) selectedExpression;

		assert navigableReference.getNavigable() instanceof EntityValuedNavigable;

		final EntityValuedNavigable navigable = (EntityValuedNavigable) navigableReference.getNavigable();
		navigable.getEntityDescriptor().createSelection(  )
		return new NavigableSelection( navigableReference, resultVariable );
	}

}
