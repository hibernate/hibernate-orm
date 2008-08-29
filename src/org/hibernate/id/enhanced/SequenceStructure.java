package org.hibernate.id.enhanced;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.HibernateException;

/**
 * Describes a sequence.
 *
 * @author Steve Ebersole
 */
public class SequenceStructure implements DatabaseStructure {
	private static final Log log = LogFactory.getLog( SequenceStructure.class );

	private final String sequenceName;
	private final int initialValue;
	private final int incrementSize;
	private final String sql;
	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;

	public SequenceStructure(Dialect dialect, String sequenceName, int initialValue, int incrementSize) {
		this.sequenceName = sequenceName;
		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		sql = dialect.getSequenceNextValString( sequenceName );
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return sequenceName;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getIncrementSize() {
		return incrementSize;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getTimesAccessed() {
		return accessCounter;
	}

	/**
	 * {@inheritDoc}
	 */
	public AccessCallback buildCallback(final SessionImplementor session) {
		return new AccessCallback() {
			public long getNextValue() {
				accessCounter++;
				try {
					PreparedStatement st = session.getBatcher().prepareSelectStatement( sql );
					try {
						ResultSet rs = st.executeQuery();
						try {
							rs.next();
							long result = rs.getLong( 1 );
							if ( log.isDebugEnabled() ) {
								log.debug("Sequence identifier generated: " + result);
							}
							return result;
						}
						finally {
							try {
								rs.close();
							}
							catch( Throwable ignore ) {
								// intentionally empty
							}
						}
					}
					finally {
						session.getBatcher().closeStatement( st );
					}

				}
				catch ( SQLException sqle) {
					throw JDBCExceptionHelper.convert(
							session.getFactory().getSQLExceptionConverter(),
							sqle,
							"could not get next sequence value",
							sql
					);
				}
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public void prepare(Optimizer optimizer) {
		applyIncrementSizeToSourceValues = optimizer.applyIncrementSizeToSourceValues();
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		int sourceIncrementSize = applyIncrementSizeToSourceValues ? incrementSize : 1;
		return dialect.getCreateSequenceStrings( sequenceName, initialValue, sourceIncrementSize );
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return dialect.getDropSequenceStrings( sequenceName );
	}
}
