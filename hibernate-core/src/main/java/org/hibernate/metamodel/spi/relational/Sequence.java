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
package org.hibernate.metamodel.spi.relational;

import org.hibernate.HibernateException;

/**
 * Models a database {@code SEQUENCE}.
 *
 * @author Steve Ebersole
 */
public class Sequence implements Exportable {
	private final ObjectName name;
	private final String nameText;
	private int initialValue = 1;
	private int incrementSize = 1;

	public Sequence(ObjectName name) {
		this.name = name;
		this.nameText = name.toText();
	}

	public Sequence(ObjectName name, int initialValue, int incrementSize) {
		this( name );
		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
	}

	public ObjectName getName() {
		return name;
	}

	@Override
	public String getExportIdentifier() {
		return nameText;
	}

	public int getInitialValue() {
		return initialValue;
	}

	public int getIncrementSize() {
		return incrementSize;
	}

	public void validate(int initialValue, int incrementSize) {
		if ( this.initialValue != initialValue ) {
			throw new HibernateException(
					String.format(
							"Multiple references to database sequence [%s] were encountered attempting to" +
									"set conflicting values for 'initial value'.  Found [%s] and [%s]",
							nameText,
							this.initialValue,
							initialValue
					)
			);
		}
		if ( this.incrementSize != incrementSize ) {
			throw new HibernateException(
					String.format(
							"Multiple references to database sequence [%s] were encountered attempting to" +
									"set conflicting values for 'increment size'.  Found [%s] and [%s]",
							nameText,
							this.incrementSize,
							incrementSize
					)
			);
		}
	}
}
