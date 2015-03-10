/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.relational;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;

/**
 * Models a database {@code SEQUENCE}.
 *
 * @author Steve Ebersole
 */
public class Sequence implements Exportable {
	public static class Name extends QualifiedNameParser.NameParts {
		public Name(
				Identifier catalogIdentifier,
				Identifier schemaIdentifier,
				Identifier nameIdentifier) {
			super( catalogIdentifier, schemaIdentifier, nameIdentifier );
		}
	}

	private final QualifiedSequenceName name;
	private final String exportIdentifier;
	private int initialValue = 1;
	private int incrementSize = 1;

	public Sequence(Identifier catalogName, Identifier schemaName, Identifier sequenceName) {
		this.name = new QualifiedSequenceName( catalogName, schemaName, sequenceName );
		this.exportIdentifier = name.render();
	}

	public Sequence(
			Identifier catalogName,
			Identifier schemaName,
			Identifier sequenceName,
			int initialValue,
			int incrementSize) {
		this( catalogName, schemaName, sequenceName );
		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
	}

	public QualifiedSequenceName getName() {
		return name;
	}

	@Override
	public String getExportIdentifier() {
		return exportIdentifier;
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
							exportIdentifier,
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
							exportIdentifier,
							this.incrementSize,
							incrementSize
					)
			);
		}
	}
}
