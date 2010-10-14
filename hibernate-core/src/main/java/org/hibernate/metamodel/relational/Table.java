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

/**
 * Models the concept of a relational <tt>TABLE</tt> (or <tt>VIEW</tt>).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Table extends AbstractTableSpecification implements ValueContainer, Exportable {
	private final ObjectName name;
	private final Set<ObjectName> spaces;

	public Table(ObjectName name) {
		this.name = name;
		this.spaces = java.util.Collections.singleton( name );
	}

	public ObjectName getObjectName() {
		return name;
	}

	@Override
	public String getLoggableValueQualifier() {
		return getObjectName().getIdentifier();
	}

	@Override
	public String getExportIdentifier() {
		return getObjectName().getIdentifier();
	}

	@Override
	public Set<ObjectName> getSpaces() {
		return spaces;
	}

	@Override
	public String toLoggableString() {
		return getObjectName().getIdentifier();
	}

	@Override
	public String toString() {
		return "Table{" +
				"name=" + getObjectName().getIdentifier() +
				'}';
	}
}
