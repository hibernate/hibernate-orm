/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.from;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.persister.common.internal.SingularAttributeBasic;
import org.hibernate.persister.common.internal.SingularAttributeEmbedded;
import org.hibernate.persister.common.internal.SingularAttributeEntity;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.SingularAttributeImplementor;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.persister.entity.spi.EntityIdentifierSimple;
import org.hibernate.persister.entity.spi.ImprovedEntityPersister;
import org.hibernate.sql.sqm.ast.expression.AttributeReference;
import org.hibernate.sql.sqm.ast.expression.EntityReference;
import org.hibernate.sql.sqm.convert.spi.NotYetImplementedException;
import org.hibernate.sqm.domain.SingularAttribute;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableGroup implements TableGroup {
	private static final Logger log = Logger.getLogger( AbstractTableGroup.class );

	private final TableSpace tableSpace;
	private final String aliasBase;

	private TableBinding rootTableBinding;
	private List<TableJoin> tableJoins;

	public AbstractTableGroup(TableSpace tableSpace, String aliasBase) {
		this.tableSpace = tableSpace;
		this.aliasBase = aliasBase;
	}

	@Override
	public TableSpace getTableSpace() {
		return tableSpace;
	}

	@Override
	public String getAliasBase() {
		return aliasBase;
	}

	public TableBinding getRootTableBinding() {
		return rootTableBinding;
	}

	public void setRootTableBinding(TableBinding rootTableBinding) {
		log.tracef(
				"Setting root TableSpecification for group [%s] : %s (was %s)",
				this.toString(),
				rootTableBinding,
				this.rootTableBinding == null ? "<null>" : this.rootTableBinding
		);
		this.rootTableBinding = rootTableBinding;
	}

	public List<TableJoin> getTableJoins() {
		if ( tableJoins == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( tableJoins );
		}
	}

	@Override
	public ColumnBinding[] resolveBindings(SingularAttribute attribute) {
		final Column[] columns;
		if ( attribute instanceof SingularAttributeBasic ) {
			columns = ( (SingularAttributeBasic) attribute ).getColumns();
		}
		else if ( attribute instanceof SingularAttributeEntity ) {
			columns = ( (SingularAttributeEntity) attribute ).getColumns();
		}
		else if ( attribute instanceof SingularAttributeEmbedded ) {
			columns = ( (SingularAttributeEmbedded) attribute ).asManagedType().collectColumns();
		}
		else if ( attribute instanceof EntityIdentifierSimple ) {
			columns = ( (EntityIdentifierSimple) attribute ).getColumns();
		}
		else {
			throw new NotYetImplementedException( "resolveBindings() : " + attribute );
		}

		final ColumnBinding[] bindings = new ColumnBinding[columns.length];
		for ( int i = 0; i < columns.length; i++ ) {
			final TableBinding tableBinding = locateTableBinding( columns[i].getSourceTable() );
			bindings[i] = new ColumnBinding( columns[i], tableBinding );
		}

		return bindings;
	}

	@Override
	public AttributeReference resolve(SingularAttribute attribute) {
		return new AttributeReference( (SingularAttributeImplementor) attribute, resolveBindings( attribute ) );
	}

	@Override
	public EntityReference resolveEntityReference() {
		final ImprovedEntityPersister improvedEntityPersister = resolveEntityReferenceBase();

		final TableBinding tableBinding = locateTableBinding( improvedEntityPersister.getRootTable() );
		final Collection<Column> columns = tableBinding.getTable().getColumns();
		ColumnBinding[] columnBindings = new ColumnBinding[columns.size()];
		final Iterator<Column> iterator = columns.iterator();
		int i = 0;
		while ( iterator.hasNext() ) {
			columnBindings[i] = new ColumnBinding( iterator.next(), tableBinding );
			i++;
		}

		return new EntityReference( improvedEntityPersister.getOrmType(), columnBindings );
	}

	protected abstract ImprovedEntityPersister resolveEntityReferenceBase();

	private TableBinding locateTableBinding(Table table) {
		if ( table == getRootTableBinding().getTable() ) {
			return getRootTableBinding();
		}

		for ( TableJoin tableJoin : getTableJoins() ) {
			if ( tableJoin.getJoinedTableBinding().getTable() == table ) {
				return tableJoin.getJoinedTableBinding();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table : " + table );
	}

	public void addTableSpecificationJoin(TableJoin join) {
		log.tracef( "Adding TableSpecification join [%s] to group [%s]", join, this );
		if ( tableJoins == null ) {
			tableJoins = new ArrayList<TableJoin>();
		}
		tableJoins.add( join );
	}
}
