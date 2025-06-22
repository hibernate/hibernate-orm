/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;

/**
 * Models a database {@code SEQUENCE}.
 *
 * @author Steve Ebersole
 */
public class Sequence implements ContributableDatabaseObject {
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
	private final String contributor;
	private final int initialValue;
	private final int incrementSize;
	private final String options;

	public Sequence(
			String contributor,
			Identifier catalogName,
			Identifier schemaName,
			Identifier sequenceName) {
		this( contributor, catalogName, schemaName, sequenceName, 1, 1, null );
	}

	public Sequence(
			String contributor,
			Identifier catalogName,
			Identifier schemaName,
			Identifier sequenceName,
			int initialValue,
			int incrementSize) {
		this( contributor, catalogName, schemaName, sequenceName, initialValue, incrementSize, null );
	}

	public Sequence(
			String contributor,
			Identifier catalogName,
			Identifier schemaName,
			Identifier sequenceName,
			int initialValue,
			int incrementSize,
			String options) {
		this.contributor = contributor;
		this.name = new QualifiedSequenceName(
				catalogName,
				schemaName,
				sequenceName
		);
		this.exportIdentifier = name.render();
		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.options = options;
	}

	public QualifiedSequenceName getName() {
		return name;
	}

	@Override
	public String getExportIdentifier() {
		return exportIdentifier;
	}

	@Override
	public String getContributor() {
		return contributor;
	}

	public int getInitialValue() {
		return initialValue;
	}

	public int getIncrementSize() {
		return incrementSize;
	}

	public String getOptions() {
		return options;
	}

	public void validate(int initialValue, int incrementSize) {
		if ( this.initialValue != initialValue ) {
			throw new HibernateException(
					String.format(
							"Multiple references to database sequence [%s] were encountered attempting to " +
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
							"Multiple references to database sequence [%s] were encountered attempting to " +
									"set conflicting values for 'increment size'.  Found [%s] and [%s]",
							exportIdentifier,
							this.incrementSize,
							incrementSize
					)
			);
		}
	}
}
