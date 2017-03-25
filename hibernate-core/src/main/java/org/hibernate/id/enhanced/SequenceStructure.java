/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Describes a sequence.
 *
 * @author Steve Ebersole
 */
public class SequenceStructure implements DatabaseStructure {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SequenceStructure.class.getName()
	);

	private final QualifiedName logicalQualifiedSequenceName;
	private final int initialValue;
	private final int incrementSize;
	private final Class numberType;

	private String sql;
	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;
	protected String sequenceName;

	public SequenceStructure(
			JdbcEnvironment jdbcEnvironment,
			QualifiedName qualifiedSequenceName,
			int initialValue,
			int incrementSize,
			Class numberType) {
		this.logicalQualifiedSequenceName = qualifiedSequenceName;

		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.numberType = numberType;
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
		buildSequence( database );
		this.sql = database.getJdbcEnvironment().getDialect().getSequenceNextValString( sequenceName );
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return dialect.getCreateSequenceStrings( sequenceName, initialValue, getSourceIncrementSize() );
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return dialect.getDropSequenceStrings( sequenceName );
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
			sequence = namespace.createSequence( logicalQualifiedSequenceName.getObjectName(), initialValue, sourceIncrementSize );
		}

		this.sequenceName = database.getJdbcEnvironment().getQualifiedObjectNameFormatter().format(
				sequence.getName(),
				database.getJdbcEnvironment().getDialect()
		);
	}
}
