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

import java.util.Set;
import java.util.HashSet;

/**
 * A <tt>data container</tt> defined by a <tt>SELECT</tt> statement.  This translates into an inline view in the
 * SQL statements: <code>select ... from (select ... from logical_table_table ...) ...</code>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class InLineView extends AbstractTableSpecification implements ValueContainer {
	private final String select;
	private final String uniqueValueQualifier;
	private Set<ObjectName> synchronizedTableSpaces = java.util.Collections.emptySet();

	public InLineView(String select, String uniqueValueQualifier) {
		this.select = select;
		this.uniqueValueQualifier = uniqueValueQualifier;
	}

	public String getSelect() {
		return select;
	}

	@Override
	public String getLoggableValueQualifier() {
		return uniqueValueQualifier;
	}

	public void addSynchronizedTable(String tableName) {
		addSynchronizedTable( new ObjectName( null, null, tableName ) );
	}

	public void addSynchronizedTable(ObjectName tableName) {
		if ( synchronizedTableSpaces.isEmpty() ) {
			synchronizedTableSpaces = new HashSet<ObjectName>();
		}
		synchronizedTableSpaces.add( tableName );
	}

	@Override
	public Set<ObjectName> getSpaces() {
		return synchronizedTableSpaces;
	}

	@Override
	public String toLoggableString() {
		return "{inline-view}";
	}
}
