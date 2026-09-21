/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.Collection;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;

/**
 * A mapping model object representing a relational database {@linkplain org.hibernate.annotations.Struct UDT}.
 */
@Incubating(since = "6.6")
public class UserDefinedObjectType extends AbstractUserDefinedType {

	private final java.util.List<Column> columns = new java.util.ArrayList<>();
	private int[] orderMapping;
	private String comment;

	public UserDefinedObjectType(String contributor, Namespace namespace, Identifier physicalTypeName) {
		super( contributor, namespace, physicalTypeName );
	}

	/**
	 * Return the column which is identified by column provided as argument.
	 *
	 * @param column column with at least a name.
	 * @return the underlying column or null if not inside this table.
	 *         Note: the instance *can* be different than the input parameter,
	 *         but the name will be the same.
	 */
	public Column getColumn(Column column) {
		if ( column == null ) {
			return null;
		}
		else {
			final var existing = getColumn( column.getPhysicalName() );
			return column.equals( existing ) ? existing : null;
		}
	}

	public Column getColumn(org.hibernate.relational.naming.spi.PhysicalName name) {
		if ( name == null ) {
			return null;
		}
		return columns.stream().filter( column -> column.getPhysicalName().equals( name ) ).findFirst().orElse( null );
	}

	public Column getColumn(int n) {
		final var iter = columns.iterator();
		for ( int i = 0; i < n - 1; i++ ) {
			iter.next();
		}
		return iter.next();
	}

	public void addColumn(Column column) {
		final Column old = getColumn( column );
		if ( old == null ) {
			columns.add( column );
			column.uniqueInteger = columns.size();
		}
		else {
			column.uniqueInteger = old.uniqueInteger;
		}
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Collection<Column> getColumns() {
		return columns;
	}

	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Internal
	public void reorderColumns(List<Column> columns) {
		if ( orderMapping != null ) {
			return;
		}
		orderMapping = new int[columns.size()];
		int i = 0;
		for ( var column : this.columns ) {
			orderMapping[columns.indexOf( column )] = i++;
		}
		this.columns.clear();
		for ( var column : columns ) {
			this.columns.add( column );
		}
	}

	@Internal
	public int[] getOrderMapping() {
		return orderMapping;
	}
}
