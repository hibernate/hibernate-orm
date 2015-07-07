package org.hibernate.boot.model.relational;

import java.util.Collection;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Table;

/**
 * Represents a named schema/catalog pair and manages objects defined within.
 *
 * @author Steve Ebersole
 */
public interface Schema {

	Schema.Name getName();

	Schema.Name getPhysicalName();

	Collection<Table> getTables();

	/**
	 * Returns the table with the specified logical table name.
	 *
	 * @param logicalTableName - the logical name of the table
	 *
	 * @return the table with the specified table name,
	 *         or null if there is no table with the specified
	 *         table name.
	 */
	Table locateTable(Identifier logicalTableName);

	/**
	 * Creates a mapping Table instance.
	 *
	 * @param logicalTableName The logical table name
	 *
	 * @return the created table.
	 */
	Table createTable(Identifier logicalTableName, boolean isAbstract);

	DenormalizedTable createDenormalizedTable(Identifier logicalTableName, boolean isAbstract, Table includedTable);

	Sequence locateSequence(Identifier name);

	Sequence createSequence(Identifier logicalName, int initialValue, int increment);

	Iterable<Sequence> getSequences();

	class Name implements Comparable<Name> {
		private final Identifier catalog;
		private final Identifier schema;

		public Name(Identifier catalog, Identifier schema) {
			this.schema = schema;
			this.catalog = catalog;
		}

		public Identifier getCatalog() {
			return catalog;
		}

		public Identifier getSchema() {
			return schema;
		}

		@Override
		public String toString() {
			return "Name" + "{catalog=" + catalog + ", schema=" + schema + '}';
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final Name that = (Name) o;

			return EqualsHelper.equals( this.catalog, that.catalog )
					&& EqualsHelper.equals( this.schema, that.schema );
		}

		@Override
		public int hashCode() {
			int result = catalog != null ? catalog.hashCode() : 0;
			result = 31 * result + (schema != null ? schema.hashCode() : 0);
			return result;
		}

		@Override
		public int compareTo(Name that) {
			// per Comparable, the incoming Name cannot be null.  However, its catalog/schema might be
			// so we need to account for that.
			int catalogCheck = ComparableHelper.compare( this.getCatalog(), that.getCatalog() );
			if ( catalogCheck != 0 ) {
				return catalogCheck;
			}

			return ComparableHelper.compare( this.getSchema(), that.getSchema() );
		}
	}

	class ComparableHelper {
		public static <T extends Comparable<T>> int compare(T first, T second) {
			if ( first == null ) {
				if ( second == null ) {
					return 0;
				}
				else {
					return 1;
				}
			}
			else {
				if ( second == null ) {
					return -1;
				}
				else {
					return first.compareTo( second );
				}
			}
		}
	}
}
