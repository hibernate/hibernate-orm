/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.relational;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.spi.ColumnInformation;
import org.hibernate.tool.schema.spi.TableInformation;

/**
 * Models the concept of a relational <tt>TABLE</tt> (or <tt>VIEW</tt>).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Table extends AbstractTableSpecification implements Exportable {
	private final Schema database;
	private Identifier physicalName;
	private Identifier logicalName;
	private ObjectName qualifiedName;
	private String exportIdentifier;

	private final Set<Index> indexes = new LinkedHashSet<Index>();
	private final Set<UniqueKey> uniqueKeys = new LinkedHashSet<UniqueKey>();
	private final List<CheckConstraint> checkConstraints = new ArrayList<CheckConstraint>();
	private final List<String> comments = new ArrayList<String>();

	/**
	 * Constructs a {@link Table} instance.
	 *
	 * @param database - the schema
	 * @param logicalName - The logical name
	 * @param physicalName - the physical table name.
	 */
	public Table(Schema database, Identifier logicalName, Identifier physicalName) {
		this.database = database;
		this.logicalName = logicalName;
		this.physicalName = physicalName;
		this.qualifiedName = new ObjectName( database, physicalName );
		this.exportIdentifier = qualifiedName.toText();
	}

	@Override
	public Schema getSchema() {
		return database;
	}

	/**
	 * Gets the logical table name.
	 *
	 * @return the logical table name.
	 */
	@Override
	public Identifier getLogicalName() {
		return logicalName;
	}

	/**
	 * Returns the simple physical name.
	 *
	 * @return The simple (non-qualfied) table name.  For the qualified name, see {@link #getTableName()}
	 *
	 * @see {@link #getTableName()}
	 */
	public Identifier getPhysicalName() {
		return physicalName;
	}

	/**
	 * Gets the qualified table name.
	 *
	 * @return the qualified table name.
	 */
	public ObjectName getTableName() {
		return qualifiedName;
	}

	@Override
	public String getLoggableValueQualifier() {
		return exportIdentifier;
	}

	@Override
	public String getExportIdentifier() {
		return exportIdentifier;
	}

	@Override
	public String toLoggableString() {
		return exportIdentifier;
	}

	@Override
	public Iterable<Index> getIndexes() {
		return Collections.unmodifiableSet( indexes );
	}

	@Override
	public Index getOrCreateIndex(String name) {
		Index result = null;
		if ( name != null ) {
			result = locateConstraint( indexes, name );
		}
		if ( result == null ) {
			result = new Index( this, name );
			indexes.add( result );
		}
		return result;
	}

	@Override
	public Iterable<UniqueKey> getUniqueKeys() {
		return Collections.unmodifiableSet( uniqueKeys );
	}

	@Override
	public UniqueKey getOrCreateUniqueKey(String name) {
		UniqueKey result = null;
		if ( name != null ) {
			result = locateConstraint( uniqueKeys, name );
		}
		if ( result == null ) {
			result = new UniqueKey( this, name );
			uniqueKeys.add( result );
		}
		return result;
	}

	@Override
	public Iterable<CheckConstraint> getCheckConstraints() {
		return checkConstraints;
	}

	@Override
	public void addCheckConstraint(String checkCondition) {
        //todo ? StringHelper.isEmpty( checkCondition );
        //todo default name?
		checkConstraints.add( new CheckConstraint( this, "", checkCondition ) );
	}

	@Override
	public Iterable<String> getComments() {
		return comments;
	}

	@Override
	public void addComment(String comment) {
		comments.add( comment );
	}

	@Override
	public String getQualifiedName(Dialect dialect) {
		return qualifiedName.toText( dialect );
	}

	@Override
	public String toString() {
		return "Table{name=" + exportIdentifier + '}';
	}

	public String[] sqlAlterStrings(TableInformation tableInformation, JdbcEnvironment jdbcEnvironment) {
		final Dialect dialect = jdbcEnvironment.getDialect();
		final String baseAlterCommand = new StringBuilder( "alter table " )
				.append( jdbcEnvironment.getQualifiedObjectNameSupport().formatName( getTableName() ) )
				.append( ' ' )
				.append( dialect.getAddColumnString() )
				.toString();

		final List<String> commands = new ArrayList<String>();

		for ( Value value : values() ) {
			if ( ! Column.class.isInstance( value ) ) {
				continue;
			}

			final Column column = (Column) value;
			final ColumnInformation columnInformation = tableInformation.getColumnInformation( column.getColumnName() );

			if ( columnInformation != null ) {
				continue;
			}

			StringBuilder alter = new StringBuilder( baseAlterCommand )
					.append( ' ' )
					.append( column.getColumnName().getText( dialect ) )
					.append( ' ' )
					.append( column.getSqlType() );


			final String defaultValue = column.getDefaultValue();
			if ( defaultValue != null ) {
				alter.append( " default " )
						.append( defaultValue );
			}

			if ( column.isNullable() ) {
				alter.append( dialect.getNullColumnString() );
			}
			else {
				alter.append( " not null" );
			}

			boolean useUniqueConstraint = column.isUnique()
					&& dialect.supportsUnique()
					&& ( !column.isNullable() || dialect.supportsNotNullUnique() );
			if ( useUniqueConstraint ) {
				alter.append( " unique" );
			}

			final String checkCondition = column.getCheckCondition();
			if ( checkCondition != null && dialect.supportsColumnCheck() ) {
				alter.append( " check(" )
						.append( checkCondition )
						.append( ")" );
			}

			final String columnComment = column.getComment();
			if ( columnComment != null ) {
				alter.append( dialect.getColumnComment( columnComment ) );
			}

			commands.add( alter.toString() );
		}

		return commands.toArray( new String[ commands.size() ] );
	}
}
