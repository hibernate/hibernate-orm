/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IntegralDataTypeHolder;

import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.id.IdentifierGeneratorHelper.getIntegralDataTypeHolder;

/**
 * Describes a sequence.
 *
 * @author Steve Ebersole
 */
public class SequenceStructure implements DatabaseStructure {

	private final String contributor;
	private final QualifiedName logicalQualifiedSequenceName;
	private final int initialValue;
	private final int incrementSize;
	private final Class<?> numberType;
	private final String options;

	private String sql;
	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;
	protected QualifiedName physicalSequenceName;

	public SequenceStructure(
			String contributor,
			QualifiedName qualifiedSequenceName,
			int initialValue,
			int incrementSize,
			Class<?> numberType) {
		this.contributor = contributor;
		this.logicalQualifiedSequenceName = qualifiedSequenceName;

		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.numberType = numberType;
		this.options = null;
	}

	public SequenceStructure(
			String contributor,
			QualifiedName qualifiedSequenceName,
			int initialValue,
			int incrementSize,
			String options,
			Class<?> numberType) {
		this.contributor = contributor;
		this.logicalQualifiedSequenceName = qualifiedSequenceName;

		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.options = options;
		this.numberType = numberType;
	}

	@Override
	public QualifiedName getPhysicalName() {
		return physicalSequenceName;
	}

	@Override
	public int getIncrementSize() {
		return incrementSize;
	}

	@Override
	public int getTimesAccessed() {
		return accessCounter;
	}

	@Override
	public int getInitialValue() {
		return initialValue;
	}

	@Override @Deprecated
	public String[] getAllSqlForTests() {
		return new String[] { sql };
	}

	@Override
	public AccessCallback buildCallback(final SharedSessionContractImplementor session) {
		if ( sql == null ) {
			throw new AssertionFailure( "SequenceStyleGenerator's SequenceStructure was not properly initialized" );
		}

		return new AccessCallback() {
			@Override
			public IntegralDataTypeHolder getNextValue() {
				accessCounter++;
				try {
					final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
					final PreparedStatement st = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
					try {
						final ResultSet rs = jdbcCoordinator.getResultSetReturn().extract( st, sql );
						try {
							rs.next();
							final IntegralDataTypeHolder value = getIntegralDataTypeHolder( numberType );
							value.initialize( rs, 1 );
							if ( JDBC_LOGGER.isTraceEnabled() ) {
								JDBC_LOGGER.sequenceValueRetrievedFromDatabase( value.makeValue() );
							}
							return value;
						}
						finally {
							try {
								jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( rs, st );
							}
							catch( Throwable ignore ) {
								// intentionally empty
							}
						}
					}
					finally {
						jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
						jdbcCoordinator.afterStatementExecution();
					}

				}
				catch ( SQLException sqle) {
					throw session.getJdbcServices().getSqlExceptionHelper().convert(
							sqle,
							"could not get next sequence value",
							sql
					);
				}
			}

			@Override
			public String getTenantIdentifier() {
				return session.getTenantIdentifier();
			}
		};
	}

	@Override
	public void configure(Optimizer optimizer) {
		applyIncrementSizeToSourceValues = optimizer.applyIncrementSizeToSourceValues();
	}

	@Override
	public void registerExportables(Database database) {
		buildSequence( database );
	}

	@Override
	public void initialize(SqlStringGenerationContext context) {
		this.sql = context.getDialect().getSequenceSupport()
				.getSequenceNextValString( context.format( physicalSequenceName ) );
	}

	@Override
	public boolean isPhysicalSequence() {
		return true;
	}

	protected final int getSourceIncrementSize() {
		return applyIncrementSizeToSourceValues ? incrementSize : 1;
	}

	protected QualifiedName getQualifiedName() {
		return logicalQualifiedSequenceName;
	}

	protected void buildSequence(Database database) {
		final int sourceIncrementSize = getSourceIncrementSize();

		final Namespace namespace = database.locateNamespace(
				logicalQualifiedSequenceName.getCatalogName(),
				logicalQualifiedSequenceName.getSchemaName()
		);
		Sequence sequence = namespace.locateSequence( logicalQualifiedSequenceName.getObjectName() );
		if ( sequence != null ) {
			sequence.validate( initialValue, sourceIncrementSize );
		}
		else {
			sequence = namespace.createSequence(
					logicalQualifiedSequenceName.getObjectName(),
					(physicalName) -> new Sequence(
							contributor,
							namespace.getPhysicalName().catalog(),
							namespace.getPhysicalName().schema(),
							physicalName,
							initialValue,
							sourceIncrementSize,
							options
					)
			);
		}

		physicalSequenceName = sequence.getName();
	}
}
