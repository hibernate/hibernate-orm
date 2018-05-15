/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedConstraint;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.dialect.Dialect;

/**
 * A relational constraint.
 *
 * @author Gavin King
 * @author Brett Meyer
 */
public abstract class Constraint implements MappedConstraint, Serializable {

	private String name;
	private final List<MappedColumn> columns = new ArrayList<>();
	private MappedTable table;

	private boolean creationEnabled = true;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public void disableCreation() {
		creationEnabled = false;
	}

	@Override
	public boolean isCreationEnabled() {
		return creationEnabled;
	}

	/**
	 * If a constraint is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 * Static so the name can be generated prior to creating the Constraint.
	 * They're cached, keyed by name, in multiple locations.
	 *
	 * @return String The generated name
	 */
	public static String generateName(String prefix, MappedTable table, List<MappedColumn> mappedColumns) {
		final List<Column> columns = mappedColumns.stream()
				.filter( column -> !column.isFormula() )
				.map( Column.class::cast )
				.collect( Collectors.toList() );

		return generateNameFromColumns( prefix, table, columns );
	}

	private static String generateNameFromColumns(String prefix, MappedTable table, List<Column> columns) {
		// Use a concatenation that guarantees uniqueness, even if identical names
		// exist between all table and column identifiers.

		StringBuilder sb = new StringBuilder( "table`" + table.getName() + "`" );

		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		List<Column> alphabeticalColumns = new ArrayList<>( columns );
		alphabeticalColumns.sort( ColumnComparator.INSTANCE );
		for ( Column column : alphabeticalColumns ) {
			String columnName = column == null ? "" : column.getName().getText();
			sb.append( "column`" ).append( columnName ).append( "`" );
		}
		return prefix + hashedName( sb.toString() );
	}

	/**
	 * Hash a constraint name using MD5. Convert the MD5 digest to base 35
	 * (full alphanumeric), guaranteeing
	 * that the length of the name will always be smaller than the 30
	 * character identifier restriction enforced by a few dialects.
	 *
	 * @param s The name to be hashed.
	 *
	 * @return String The hased name.
	 */
	public static String hashedName(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance( "MD5" );
			md.reset();
			md.update( s.getBytes() );
			byte[] digest = md.digest();
			BigInteger bigInt = new BigInteger( 1, digest );
			// By converting to base 35 (full alphanumeric), we guarantee
			// that the length of the name will always be smaller than the 30
			// character identifier restriction enforced by a few dialects.
			return bigInt.toString( 35 );
		}
		catch (NoSuchAlgorithmException e) {
			throw new HibernateException( "Unable to generate a hashed Constraint name!", e );
		}
	}

	public static class ColumnComparator implements Comparator<Column> {
		public static ColumnComparator INSTANCE = new ColumnComparator();

		public int compare(Column col1, Column col2) {
			return col1.getName().compareTo( col2.getName() );
		}
	}

	/**
	 * @deprecated Use {@link Constraint#addColumn(MappedColumn)}
	 * instead.  We want to create "logical constraints", regardless of whether they
	 * are "exportable" (a real, physical constraint).  So we open up the type of
	 * "columns" we accept here.
	 */
	@Deprecated
	public void addColumn(Column column) {
		addColumn( (MappedColumn) column );
	}

	@Override
	public void addColumn(MappedColumn column) {
		if ( !columns.contains( column ) ) {
			columns.add( column );

			if ( column.isFormula() ) {
				disableCreation();
			}
		}
	}

	/**
	 * @deprecated since 6.0, use {@link #addColumns(List)} )}.
	 */
	@Deprecated
	public void addColumns(Iterator columnIterator) {
		while ( columnIterator.hasNext() ) {
			addColumn( (MappedColumn) columnIterator.next() );
		}
	}

	@Override
	public void addColumns(List<? extends MappedColumn> columns) {
		columns.stream().forEach( column -> addColumn( (Selectable) column ) );
	}

	/**
	 * @return true if this constraint already contains a column with same name.
	 */
	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	@Override
	public Column getColumn(int i) {
		return (Column) columns.get( i );
	}

	@SuppressWarnings("unchecked")
	<T> Iterator<T> cast(Iterator itr) {
		return itr;
	}

	/**
	 * @deprecated since 6.0 use {@link #getMappedTable()}.
	 */
	@Deprecated
	public Table getTable() {
		return (Table) getMappedTable();
	}

	/**
	 * @deprecated since 6.0, use {@link #setMappedTable(MappedTable)}.
	 */
	@Deprecated
	public void setTable(MappedTable table) {
		setMappedTable( table );
	}

	@Override
	public MappedTable getMappedTable() {
		return table;
	}

	@Override
	public void setMappedTable(MappedTable table) {
		this.table = table;
	}

	public boolean isGenerated(Dialect dialect) {
		return true;
	}

	@Override
	public List<MappedColumn> getColumns() {
		return cast( columns );
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> cast(List values) {
		return values;
	}


	public String toString() {
		return getClass().getName() + '(' + getMappedTable().getName() + getColumns() + ") as " + name;
	}
}
