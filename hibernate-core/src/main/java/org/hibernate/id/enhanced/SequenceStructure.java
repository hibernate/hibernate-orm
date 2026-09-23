/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.mapping.PhysicalTable;

import static org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper.logicalName;

import java.io.Serializable;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.relational.naming.internal.QualifiedPhysicalNameSnapshot;
import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.id.IdentifierGeneratorHelper.extractLong;
import static org.hibernate.id.enhanced.ResyncHelper.getNextSequenceValue;
import static org.hibernate.id.enhanced.ResyncHelper.getMaxPrimaryKey;

/**
 * Describes a sequence.
 *
 * @author Steve Ebersole
 */
public class SequenceStructure implements DatabaseStructure, Serializable {

	private final String contributor;
	private final QualifiedName logicalQualifiedSequenceName;
	private final int initialValue;
	private final int incrementSize;
	private final String options;

	private String sql;
	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;
	protected transient QualifiedPhysicalName physicalSequenceName;
	private QualifiedPhysicalNameSnapshot physicalNameSnapshot;

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
		}

	@Override
	public QualifiedPhysicalName getPhysicalName() {
		if ( physicalSequenceName == null && physicalNameSnapshot != null ) {
			throw new IllegalStateException( "Generator structure has not been initialized after restoration" );
		}
		return physicalSequenceName;
	}

	private QualifiedPhysicalName physicalName(SqlStringGenerationContext context) {
		if ( physicalSequenceName == null && physicalNameSnapshot != null ) {
			physicalSequenceName = physicalNameSnapshot.restore( context.getPhysicalNameFactory() );
		}
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
				public long getNextValue() {
					accessCounter++;
					try {
					final var jdbcCoordinator = session.getJdbcCoordinator();
					final var statement = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
					final var resourceRegistry = jdbcCoordinator.getLogicalConnection().getResourceRegistry();
					try {
						final var resultSet = jdbcCoordinator.getResultSetReturn().extract( statement, sql );
							try {
								resultSet.next();
								final long value = extractLong( resultSet, 1 );
								if ( JDBC_LOGGER.isTraceEnabled() ) {
									JDBC_LOGGER.sequenceValueRetrievedFromDatabase( value );
								}
								return value;
						}
						finally {
							try {
								resourceRegistry.release( resultSet, statement );
							}
							catch( Throwable ignore ) {
								// intentionally empty
							}
						}
					}
					finally {
						resourceRegistry.release( statement );
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
		sql = context.getDialect().getSequenceSupport()
				.getSequenceNextValString( context.format( physicalName( context ) ) );
	}

	@Override
	public void registerExtraExportables(PhysicalTable table, Optimizer optimizer) {
		final var optimizerState = new OptimizerResetState( optimizer );
		table.addResyncCommand( (sqlContext, isolator) -> {
			final String sequenceName = sqlContext.format( physicalName( sqlContext ) );
			final String tableName = table.getTableExpression( sqlContext );
			final String primaryKeyColumnName = table.getPrimaryKey().getColumn( 0 ).getName();
			final long max = getMaxPrimaryKey( isolator, primaryKeyColumnName, tableName );
			final long current = getNextSequenceValue( isolator, sequenceName);
			final long startWith = Math.max( max + optimizerState.adjustment(), current );
			optimizerState.reset();
			return new InitCommand( sqlContext.getDialect().getSequenceSupport()
					.getRestartSequenceString( sequenceName, startWith ) );
		} );
		table.addResetCommand( sqlContext -> {
			optimizerState.reset();
			final String sequenceName = sqlContext.format( physicalName( sqlContext ) );
			return new InitCommand( sqlContext.getDialect().getSequenceSupport()
					.getRestartSequenceString( sequenceName, initialValue ) );
		} );
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
		final var sequence =
				locateOrCreateSequence( database.locateNamespace(
						logicalName( logicalQualifiedSequenceName.getCatalogName() ),
						logicalName( logicalQualifiedSequenceName.getSchemaName() )
				) );
		physicalSequenceName = sequence.getName();
		physicalNameSnapshot = QualifiedPhysicalNameSnapshot.from( physicalSequenceName );
	}

	private Sequence locateOrCreateSequence(Namespace namespace) {
		final int sourceIncrementSize = getSourceIncrementSize();
		final var objectName = logicalQualifiedSequenceName.getObjectName();
		final var existingSequence = namespace.locateSequence( logicalName( objectName ) );
		if ( existingSequence != null ) {
			existingSequence.validate( initialValue, sourceIncrementSize );
			return existingSequence;
		}
		else {
			return namespace.createSequence(
					logicalName( objectName ),
					physicalName -> new Sequence(
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
	}
}
