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
package org.hibernate.metamodel.relational;

import java.util.Collections;

import org.hibernate.dialect.Dialect;

/**
 * A <tt>data container</tt> defined by a <tt>SELECT</tt> statement.  This translates into an inline view in the
 * SQL statements: <code>select ... from (select ... from logical_table_table ...) ...</code>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class InLineView extends AbstractTableSpecification {
	private final Schema schema;
	private final String logicalName;
	private final String select;

	public InLineView(Schema schema, String logicalName, String select) {
		this.schema = schema;
		this.logicalName = logicalName;
		this.select = select;
	}

	public Schema getSchema() {
		return schema;
	}

	public String getSelect() {
		return select;
	}

	@Override
	public String getLoggableValueQualifier() {
		return logicalName;
	}

	@Override
	public Iterable<Index> getIndexes() {
		return Collections.emptyList();
	}

	@Override
	public Index getOrCreateIndex(String name) {
		throw new UnsupportedOperationException( "Cannot create index on inline view" );
	}

	@Override
	public Iterable<UniqueKey> getUniqueKeys() {
		return Collections.emptyList();
	}

	@Override
	public UniqueKey getOrCreateUniqueKey(String name) {
		throw new UnsupportedOperationException( "Cannot create unique-key on inline view" );
	}

	@Override
	public Iterable<CheckConstraint> getCheckConstraints() {
		return Collections.emptyList();
	}

	@Override
	public void addCheckConstraint(String checkCondition) {
		throw new UnsupportedOperationException( "Cannot create check constraint on inline view" );
	}

	@Override
	public Iterable<String> getComments() {
		return Collections.emptyList();
	}

	@Override
	public void addComment(String comment) {
		throw new UnsupportedOperationException( "Cannot comment on inline view" );
	}

	@Override
	public String getQualifiedName(Dialect dialect) {
		return new StringBuilder( select.length() + 4 )
				.append( "( " )
				.append( select )
				.append( " )" )
				.toString();
	}

	@Override
	public String toLoggableString() {
		return "{inline-view}";
	}
}
