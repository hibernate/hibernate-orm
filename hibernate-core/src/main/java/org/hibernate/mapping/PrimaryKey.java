/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

/**
 * A mapping model object representing a primary key constraint.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PrimaryKey extends Constraint {
	private static final Logger log = Logger.getLogger( PrimaryKey.class );

	private UniqueKey orderingUniqueKey = null;
	private int[] originalOrder;

	public PrimaryKey(Table table){
		setTable( table );
	}

	@Override
	public void addColumn(Column column) {
		for ( Column next : getTable().getColumns() ) {
			if ( next.getCanonicalName().equals( column.getCanonicalName() ) ) {
				next.setNullable( false );
				if ( log.isDebugEnabled() ) {
					log.debugf(
							"Forcing column [%s] to be non-null as it is part of the primary key for table [%s]",
							column.getCanonicalName(),
							getTableNameForLogging( column )
					);
				}
			}
		}
		super.addColumn( column );
	}

	protected String getTableNameForLogging(Column column) {
		if ( getTable() != null ) {
			if ( getTable().getNameIdentifier() != null ) {
				return getTable().getNameIdentifier().getCanonicalName();
			}
			else {
				return "<unknown>";
			}
		}
		else if ( column.getValue() != null && column.getValue().getTable() != null ) {
			return column.getValue().getTable().getNameIdentifier().getCanonicalName();
		}
		return "<unknown>";
	}

	public String sqlConstraintString(Dialect dialect) {
		StringBuilder buf = new StringBuilder();
		if ( orderingUniqueKey != null && orderingUniqueKey.isNameExplicit() ) {
			buf.append( "constraint " ).append( orderingUniqueKey.getName() ).append( ' ' );
		}
		buf.append( "primary key (" );
		boolean first = true;
		for ( Column column : getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				buf.append(", ");
			}
			buf.append( column.getQuotedName( dialect ) );
		}
		return buf.append(')').toString();
	}

	@Override @Deprecated(since="6.2", forRemoval = true)
	public String sqlConstraintString(SqlStringGenerationContext context, String constraintName, String defaultCatalog, String defaultSchema) {
		Dialect dialect = context.getDialect();
		StringBuilder buf = new StringBuilder();
		buf.append( dialect.getAddPrimaryKeyConstraintString( constraintName ) ).append('(');
		boolean first = true;
		for ( Column column : getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				buf.append(", ");
			}
			buf.append( column.getQuotedName( dialect ) );
		}
		return buf.append(')').toString();
	}

	@Deprecated(forRemoval = true)
	public String generatedConstraintNamePrefix() {
		return "PK_";
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getExportIdentifier(), "PK-" + getName() );
	}

	public List<Column> getColumnsInOriginalOrder() {
		if ( originalOrder == null ) {
			return getColumns();
		}
		final List<Column> columns = getColumns();
		final Column[] columnsInOriginalOrder = new Column[columns.size()];
		for ( int i = 0; i < columnsInOriginalOrder.length; i++ ) {
			columnsInOriginalOrder[originalOrder[i]] = columns.get( i );
		}
		return Arrays.asList( columnsInOriginalOrder );
	}

	public void setOrderingUniqueKey(UniqueKey uniqueKey) {
		this.orderingUniqueKey = uniqueKey;
	}

	public UniqueKey getOrderingUniqueKey() {
		return this.orderingUniqueKey;
	}

	@Internal
	public void reorderColumns(List<Column> reorderedColumns) {
		if ( originalOrder != null ) {
			assert getColumns().equals( reorderedColumns );
			return;
		}
		assert getColumns().size() == reorderedColumns.size() && getColumns().containsAll( reorderedColumns );
		final List<Column> columns = getColumns();
		originalOrder = new int[columns.size()];
		List<Column> newColumns = getOrderingUniqueKey() != null ? getOrderingUniqueKey().getColumns() : reorderedColumns;
		for ( int i = 0; i < newColumns.size(); i++ ) {
			final Column reorderedColumn = newColumns.get( i );
			originalOrder[i] = columns.indexOf( reorderedColumn );
		}
		columns.clear();
		columns.addAll( newColumns );
	}

	@Internal
	public int[] getOriginalOrder() {
		return originalOrder;
	}
}
