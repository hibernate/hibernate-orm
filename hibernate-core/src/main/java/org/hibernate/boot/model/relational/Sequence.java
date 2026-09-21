/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.relational.naming.internal.QualifiedPhysicalNameSnapshot;

/**
 * Models a database {@code SEQUENCE}.
 *
 * @author Steve Ebersole
 */
public class Sequence implements ContributableDatabaseObject, Serializable {

	private transient QualifiedPhysicalName name;
	private final QualifiedPhysicalNameSnapshot nameSnapshot;
	private final String exportIdentifier;
	private final String contributor;
	private final int initialValue;
	private final int incrementSize;
	private final String options;

	public Sequence(
			String contributor,
			PhysicalName catalogName,
			PhysicalName schemaName,
			PhysicalName sequenceName) {
		this( contributor, catalogName, schemaName, sequenceName, 1, 1, null );
	}

	public Sequence(
			String contributor,
			PhysicalName catalogName,
			PhysicalName schemaName,
			PhysicalName sequenceName,
			int initialValue,
			int incrementSize) {
		this( contributor, catalogName, schemaName, sequenceName, initialValue, incrementSize, null );
	}

	public Sequence(
			String contributor,
			PhysicalName catalogName,
			PhysicalName schemaName,
			PhysicalName sequenceName,
			int initialValue,
			int incrementSize,
			String options) {
		this.contributor = contributor;
		this.name = new QualifiedPhysicalName( catalogName, schemaName, sequenceName );
		nameSnapshot = QualifiedPhysicalNameSnapshot.from( name );
		this.exportIdentifier = name.render();
		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.options = options;
	}

	public QualifiedPhysicalName getName() {
		if ( name == null ) {
			throw new IllegalStateException( "Sequence services must be reattached before accessing physical names" );
		}
		return name;
	}

	void reattach(PhysicalName.Factory factory) {
		name = nameSnapshot.restore( factory );
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
							"Multiple generators using the database sequence '%s' are defined, with conflicting 'initialValue' specifications: %s, %s",
							exportIdentifier,
							this.initialValue,
							initialValue
					)
			);
		}
		if ( this.incrementSize != incrementSize ) {
			throw new HibernateException(
					String.format(
							"Multiple generators using the database sequence '%s' are defined, with conflicting 'allocationSize' specifications: %s, %s",
							exportIdentifier,
							this.incrementSize,
							incrementSize
					)
			);
		}
	}
}
