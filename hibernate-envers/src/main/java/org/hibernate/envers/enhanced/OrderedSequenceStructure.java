/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.enhanced;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Logger;

/**
 * @author Chris Cranford
 */
public class OrderedSequenceStructure implements DatabaseStructure {

	// @todo: a fair bit of duplication from SequenceStructure - needs refactor

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SequenceStructure.class.getName()
	);

	private final QualifiedName logicalQualifiedSequenceName;
	private final int initialValue;
	private final int incrementSize;
	private final Class numberType;

	private String sequenceName;
	private String sql;
	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;
	private AuxiliaryDatabaseObject sequenceObject;

	public OrderedSequenceStructure(
			JdbcEnvironment jdbcEnvironment,
			QualifiedName qualifiedSequenceName,
			int initialValue,
			int incrementSize,
			Class numberType) {
		this.logicalQualifiedSequenceName = qualifiedSequenceName;

		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.numberType = numberType;
		this.sequenceObject = new OrderedSequence();
	}

	@Override
	public String getName() {
		return sequenceName;
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
					final PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
					try {
						final ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st );
						try {
							rs.next();
							final IntegralDataTypeHolder value = IdentifierGeneratorHelper.getIntegralDataTypeHolder( numberType );
							value.initialize( rs, 1 );
							if ( LOG.isDebugEnabled() ) {
								LOG.debugf( "Sequence value obtained: %s", value.makeValue() );
							}
							return value;
						}
						finally {
							try {
								session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, st );
							}
							catch( Throwable ignore ) {
								// intentionally empty
							}
						}
					}
					finally {
						session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
						session.getJdbcCoordinator().afterStatementExecution();
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
	public void prepare(Optimizer optimizer) {
		applyIncrementSizeToSourceValues = optimizer.applyIncrementSizeToSourceValues();
	}

	@Override
	public void registerExportables(Database database) {
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		// construct the next value sql
		this.sequenceName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				logicalQualifiedSequenceName,
				dialect
		);
		this.sql = jdbcEnvironment.getDialect().getSequenceNextValString( sequenceName );

		// add auxiliary object
		database.addAuxiliaryDatabaseObject( sequenceObject );
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		// delegate to auxiliary object
		return sequenceObject.sqlCreateStrings( dialect );
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		// delegate to auxiliary object
		return sequenceObject.sqlDropStrings( dialect );
	}

	@Override
	public boolean isPhysicalSequence() {
		return true;
	}

	private class OrderedSequence implements AuxiliaryDatabaseObject {
		@Override
		public String getExportIdentifier() {
			return logicalQualifiedSequenceName.getObjectName().getText();
		}

		@Override
		public boolean appliesToDialect(Dialect dialect) {
			return true;
		}

		@Override
		public boolean beforeTablesOnCreation() {
			return true;
		}

		@Override
		public String[] sqlCreateStrings(Dialect dialect) {
			final int sourceIncrementSize = applyIncrementSizeToSourceValues ? incrementSize : 1;

			final String[] createStrings = dialect.getCreateSequenceStrings(
					sequenceName,
					initialValue,
					sourceIncrementSize
			);

			if ( dialect instanceof Oracle8iDialect ) {
				for ( int i = 0; i < createStrings.length; ++i ) {
					createStrings[ i ] = createStrings[ i ] + " ORDER";
				}
			}

			return createStrings;
		}

		@Override
		public String[] sqlDropStrings(Dialect dialect) {
			return dialect.getDropSequenceStrings( sequenceName );
		}
	}
}
