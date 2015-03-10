/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.id;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.mapping.ValueVisitor;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class ExportableColumn extends Column {

	public ExportableColumn(Database database, Table table, String name, BasicType type) {
		this(
				database,
				table,
				name,
				type,
				database.getDialect().getTypeName( type.sqlTypes( null )[0] )
		);
	}

	public ExportableColumn(
			Database database,
			Table table,
			String name,
			BasicType type,
			String dbTypeDeclaration) {
		super( name );
		setValue( new ValueImpl( this, table, type ) );
		setSqlType( dbTypeDeclaration );
	}

	public static class ValueImpl implements Value {
		private final ExportableColumn column;
		private final Table table;
		private final BasicType type;

		public ValueImpl(ExportableColumn column, Table table, BasicType type) {
			this.column = column;
			this.table = table;
			this.type = type;
		}

		@Override
		public int getColumnSpan() {
			return 1;
		}

		@Override
		public Iterator<Selectable> getColumnIterator() {
			return new ColumnIterator( column );
		}

		@Override
		public Type getType() throws MappingException {
			return type;
		}

		@Override
		public FetchMode getFetchMode() {
			return null;
		}

		@Override
		public Table getTable() {
			return table;
		}

		@Override
		public boolean hasFormula() {
			return false;
		}

		@Override
		public boolean isAlternateUniqueKey() {
			return false;
		}

		@Override
		public boolean isNullable() {
			return false;
		}

		@Override
		public boolean[] getColumnUpdateability() {
			return new boolean[] { true };
		}

		@Override
		public boolean[] getColumnInsertability() {
			return new boolean[] { true };
		}

		@Override
		public void createForeignKey() throws MappingException {
		}

		@Override
		public boolean isSimpleValue() {
			return true;
		}

		@Override
		public boolean isValid(Mapping mapping) throws MappingException {
			return false;
		}

		@Override
		public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		}

		@Override
		public Object accept(ValueVisitor visitor) {
			return null;
		}
	}

	public static class ColumnIterator implements Iterator<Selectable> {
		private final ExportableColumn column;
		private int count = 0;

		public ColumnIterator(ExportableColumn column) {
			this.column = column;
		}

		@Override
		public boolean hasNext() {
			return count == 0;
		}

		@Override
		public ExportableColumn next() {
			if ( count > 0 ) {
				throw new NoSuchElementException( "The single element has already been read" );
			}
			count++;
			return column;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException( "Cannot remove" );
		}
	}
}
