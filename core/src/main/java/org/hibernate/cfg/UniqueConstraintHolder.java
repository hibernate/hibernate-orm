/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
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
package org.hibernate.cfg;

/**
 * {@link javax.persistence.UniqueConstraint} annotations are handled via second pass.  I do not
 * understand the reasons why at this time, so here I use a holder object to hold the information
 * needed to create the unique constraint.  The ability to name it is new, and so the code used to
 * simply keep this as a String array (the column names).
 *
 * @author Steve Ebersole
 */
public class UniqueConstraintHolder {
	private String name;
	private String[] columns;

	public String getName() {
		return name;
	}

	public UniqueConstraintHolder setName(String name) {
		this.name = name;
		return this;
	}

	public String[] getColumns() {
		return columns;
	}

	public UniqueConstraintHolder setColumns(String[] columns) {
		this.columns = columns;
		return this;
	}
}
