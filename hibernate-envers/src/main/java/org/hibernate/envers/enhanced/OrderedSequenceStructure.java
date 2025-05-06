/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.enhanced;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.enhanced.SequenceStructure;

/**
 * Describes a sequence that supports ordered sequences.
 *
 * @author Chris Cranford
 */
public class OrderedSequenceStructure extends SequenceStructure {

	private final AuxiliaryDatabaseObject sequenceObject;
	private final String suffix;

	public OrderedSequenceStructure(
			JdbcEnvironment jdbcEnvironment,
			QualifiedName qualifiedSequenceName,
			int initialValue,
			int incrementSize,
			Class<?> numberType) {
		this( jdbcEnvironment, qualifiedSequenceName, initialValue, incrementSize, false, numberType );
	}

	public OrderedSequenceStructure(
			JdbcEnvironment jdbcEnvironment,
			QualifiedName qualifiedSequenceName,
			int initialValue,
			int incrementSize,
			boolean noCache,
			Class<?> numberType) {
		super( "envers", qualifiedSequenceName, initialValue, incrementSize, numberType );
		this.sequenceObject = new OrderedSequence();
		final Dialect dialect = jdbcEnvironment.getDialect();
		if ( dialect instanceof OracleDialect ) {
			this.suffix = ( noCache ? " NOCACHE" : "" ) + " ORDER";
		}
		else {
			this.suffix = null;
		}
	}

	@Override
	protected void buildSequence(Database database) {
		database.addAuxiliaryDatabaseObject( sequenceObject );
		this.physicalSequenceName = getQualifiedName();
	}

	private class OrderedSequence implements AuxiliaryDatabaseObject {
		@Override
		public String getExportIdentifier() {
			return getQualifiedName().render();
		}

		@Override
		public boolean appliesToDialect(Dialect dialect) {
			// applies to all dialects
			// sqlCreateStrings applies dialect specific changes
			return true;
		}

		@Override
		public boolean beforeTablesOnCreation() {
			return true;
		}

		@Override
		public String[] sqlCreateStrings(SqlStringGenerationContext context) {
			Dialect dialect = context.getDialect();
			final String[] createStrings = dialect.getSequenceSupport().getCreateSequenceStrings(
					context.format( getPhysicalName() ),
					getInitialValue(),
					getSourceIncrementSize()
			);

			if ( suffix != null ) {
				for ( int i = 0; i < createStrings.length; ++i ) {
					createStrings[i] = createStrings[i] + suffix;
				}
			}

			return createStrings;
		}

		@Override
		public String[] sqlDropStrings(SqlStringGenerationContext context) {
			Dialect dialect = context.getDialect();
			return dialect.getSequenceSupport().getDropSequenceStrings( context.format( getPhysicalName() ) );
		}
	}
}
