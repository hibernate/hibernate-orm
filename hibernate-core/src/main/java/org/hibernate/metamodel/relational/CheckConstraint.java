/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.relational;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class CheckConstraint {
	private final Table table;
	private String name;
	private String condition;

	public CheckConstraint(Table table) {
		this.table = table;
	}

	public CheckConstraint(Table table, String name, String condition) {
		this.table = table;
		this.name = name;
		this.condition = condition;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	/**
	 * Obtain the table to which this constraint applies.
	 *
	 * @return The constrained table.
	 */
	public Table getTable() {
		return table;
	}

	/**
	 * Obtain the constraint name.
	 *
	 * @return the name.
	 */
	public String getName() {
		return name;
	}
}
