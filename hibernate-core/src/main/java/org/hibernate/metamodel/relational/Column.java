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

import org.hibernate.metamodel.relational.state.ColumnRelationalState;

/**
 * Models a physical column
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Column extends AbstractSimpleValue implements SimpleValue {
	private final Identifier name;
	private boolean nullable;
	private boolean unique;

	private String defaultValue;
	private String checkCondition;
	private String sqlType;

	private String readFragment;
	private String writeFragment;

	private String comment;

	private Size size = new Size();

	protected Column(TableSpecification table, int position, String name) {
		this( table, position, Identifier.toIdentifier( name ) );
	}

	protected Column(TableSpecification table, int position, Identifier name) {
		super( table, position );
		this.name = name;
	}

	public void initialize(ColumnRelationalState state, boolean forceNonNullable, boolean forceUnique) {
		size.initialize( state.getSize() );
		nullable = ! forceNonNullable &&  state.isNullable();
		unique = ! forceUnique && state.isUnique();
		checkCondition = state.getCheckCondition();
		defaultValue = state.getDefault();
		sqlType = state.getSqlType();

		// TODO: this should go into binding instead (I think???)
		writeFragment = state.getCustomWriteFragment();
		readFragment = state.getCustomReadFragment();
		comment = state.getComment();
		for ( String uniqueKey : state.getUniqueKeys() ) {
			getTable().getOrCreateUniqueKey( uniqueKey ).addColumn( this );
		}
		for ( String index : state.getIndexes() ) {
			getTable().getOrCreateIndex( index ).addColumn( this );
		}
	}

	public Identifier getName() {
		return name;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getCheckCondition() {
		return checkCondition;
	}

	public void setCheckCondition(String checkCondition) {
		this.checkCondition = checkCondition;
	}

	public String getSqlType() {
		return sqlType;
	}

	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}

	public String getReadFragment() {
		return readFragment;
	}

	public void setReadFragment(String readFragment) {
		this.readFragment = readFragment;
	}

	public String getWriteFragment() {
		return writeFragment;
	}

	public void setWriteFragment(String writeFragment) {
		this.writeFragment = writeFragment;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Size getSize() {
		return size;
	}

	public void setSize(Size size) {
		this.size = size;
	}

	@Override
	public String toLoggableString() {
		return getTable().getLoggableValueQualifier() + '.' + getName();
	}
}
