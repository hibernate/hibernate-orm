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


/**
 * Models a database {@code SEQUENCE}.
 *
 * @author Steve Ebersole
 */
public class Sequence implements Exportable {
	private final ObjectName name;
	private final int initialValue;
	private final int incrementSize;

	public Sequence(ObjectName name, int initialValue, int incrementSize) {
		this.name = name;
		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
	}

	public String getExportIdentifier() {
		return name.getIdentifier();
	}

	public ObjectName getName() {
		return name;
	}

	public int getInitialValue() {
		return initialValue;
	}

	public int getIncrementSize() {
		return incrementSize;
	}
}
