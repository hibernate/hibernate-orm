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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;

/**
 * A mapping model object representing a constraint on a relational database table.
 *
 * @author Gavin King
 * @author Brett Meyer
 */
public abstract class Constraint implements Exportable, Serializable {

	private String name;
	private final ArrayList<Column> columns = new ArrayList<>();
	private Table table;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * If a constraint is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 * Static so the name can be generated prior to creating the Constraint.
	 * They're cached, keyed by name, in multiple locations.
	 *
	 * @return String The generated name
	 *
	 * @deprecated This method does not respect the {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	public static String generateName(String prefix, Table table, Column... columns) {
		// Use a concatenation that guarantees uniqueness, even if identical names
		// exist between all table and column identifiers.
		final StringBuilder sb = new StringBuilder( "table`" + table.getName() + "`" );
		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		final Column[] alphabeticalColumns = columns.clone();
		Arrays.sort( alphabeticalColumns, Comparator.comparing( Column::getName ) );
		for ( Column column : alphabeticalColumns ) {
			final String columnName = column == null ? "" : column.getName();
			sb.append( "column`" ).append( columnName ).append( "`" );
		}
		return prefix + hashedName( sb.toString() );
	}

	/**
	 * Helper method for {@link #generateName(String, Table, Column...)}.
	 *
	 * @return String The generated name
	 *
	 * @deprecated This method does not respect the {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	public static String generateName(String prefix, Table table, List<Column> columns) {
		// N.B. legacy APIs are involved: can't trust that the columns List is actually
		// containing Column instances - the generic type isn't consistently enforced.
		// So some elements might be Formula instances, but they don't need to be part
		// of the name generation.
		final Column[] defensive = columns.stream()
				.filter( (Object thing) -> thing instanceof Column )
				.toArray( Column[]::new );
		return generateName( prefix, table, defensive);
	}

	/**
	 * Hash a constraint name using MD5. Convert the MD5 digest to base 35
	 * (full alphanumeric), guaranteeing
	 * that the length of the name will always be smaller than the 30
	 * character identifier restriction enforced by a few dialects.
	 *
	 * @param name The name to be hashed.
	 * @return String The hashed name.
	 *
	 * @deprecated Only used from deprecated methods
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	public static String hashedName(String name) {
		try {
			final MessageDigest md = MessageDigest.getInstance( "MD5" );
			md.reset();
			md.update( name.getBytes() );
			final byte[] digest = md.digest();
			final BigInteger bigInt = new BigInteger( 1, digest );
			// By converting to base 35 (full alphanumeric), we guarantee
			// that the length of the name will always be smaller than the 30
			// character identifier restriction enforced by a few dialects.
			return bigInt.toString( 35 );
		}
		catch ( NoSuchAlgorithmException e ) {
			throw new HibernateException( "Unable to generate a hashed Constraint name", e );
		}
	}

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) {
			columns.add( column );
		}
	}

	public void addColumns(Value value) {
		for ( Selectable selectable : value.getSelectables() ) {
			if ( selectable.isFormula() ) {
				throw new MappingException( "constraint involves a formula: " + name );
			}
			else {
				addColumn( (Column) selectable );
			}
		}
	}

	/**
	 * @return true if this constraint already contains a column with same name.
	 */
	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Column getColumn(int i) {
		return columns.get( i );
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	/**
	 * @deprecated No longer used
	 */
	@Deprecated(forRemoval = true)
	public boolean isGenerated(Dialect dialect) {
		return true;
	}

	public List<Column> getColumns() {
		return columns;
	}

	/**
	 * @deprecated this method is no longer called
	 */
	@Deprecated(since="6.2", forRemoval = true)
	public abstract String sqlConstraintString(
			SqlStringGenerationContext context,
			String constraintName,
			String defaultCatalog,
			String defaultSchema);

	public String toString() {
		return getClass().getSimpleName() + '(' + getTable().getName() + getColumns() + ") as " + name;
	}

	/**
	 * @return String The prefix to use in generated constraint names.  Examples:
	 * "UK_", "FK_", and "PK_".
	 * @deprecated No longer used, should be removed
	 */
	@Deprecated(since="6.5", forRemoval = true)
	public abstract String generatedConstraintNamePrefix();
}
