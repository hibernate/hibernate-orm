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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Models the concept of a relational <tt>TABLE</tt> (or <tt>VIEW</tt>).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Table extends AbstractTableSpecification implements ValueContainer, Exportable {
	private final Schema database;
	private final Identifier tableName;
	private final String qualifiedName;

	private LinkedHashMap<String,Index> indexes;
	private LinkedHashMap<String,UniqueKey> uniqueKeys;
	private List<String> checkConstraints;
	private Set<String> comments;

	public Table(Schema database, String tableName) {
		this( database, Identifier.toIdentifier( tableName ) );
	}

	public Table(Schema database, Identifier tableName) {
		this.database = database;
		this.tableName = tableName;
		ObjectName objectName = new ObjectName( database.getName().getSchema(), database.getName().getCatalog(), tableName );
		this.qualifiedName = objectName.toText();
	}

	@Override
	public Schema getSchema() {
		return database;
	}

	public Identifier getTableName() {
		return tableName;
	}

	@Override
	public String getLoggableValueQualifier() {
		return qualifiedName;
	}

	@Override
	public String getExportIdentifier() {
		return qualifiedName;
	}

	@Override
	public String toLoggableString() {
		return qualifiedName;
	}

	@Override
	public Iterable<Index> getIndexes() {
		return indexes.values();
	}

	public Index getOrCreateIndex(String name) {
		if(indexes!=null && indexes.containsKey( name )){
			return indexes.get( name );
		}
		Index index = new Index( this, name );
		if ( indexes == null ) {
			indexes = new LinkedHashMap<String,Index>();
		}
		indexes.put(name, index );
		return index;
	}

	@Override
	public Iterable<UniqueKey> getUniqueKeys() {
		return uniqueKeys.values();
	}

	public UniqueKey getOrCreateUniqueKey(String name) {
		if(uniqueKeys!=null && uniqueKeys.containsKey( name )){
			return uniqueKeys.get( name );
		}
		UniqueKey uniqueKey = new UniqueKey( this, name );
		if ( uniqueKeys == null ) {
			uniqueKeys = new LinkedHashMap<String,UniqueKey>();
		}
		uniqueKeys.put(name, uniqueKey );
		return uniqueKey;
	}

	@Override
	public Iterable<String> getCheckConstraints() {
		return checkConstraints;
	}

	@Override
	public void addCheckConstraint(String checkCondition) {
		if ( checkConstraints == null ) {
			checkConstraints = new ArrayList<String>();
		}
		checkConstraints.add( checkCondition );
	}

	@Override
	public Iterable<String> getComments() {
		return comments;
	}

	@Override
	public void addComment(String comment) {
		if ( comments == null ) {
			comments = new HashSet<String>();
		}
		comments.add( comment );
	}

	@Override
	public String toString() {
		return "Table{name=" + qualifiedName + '}';
	}
}
