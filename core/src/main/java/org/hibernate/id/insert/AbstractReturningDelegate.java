package org.hibernate.id.insert;

import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.pretty.MessageHelper;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Abstract InsertGeneratedIdentifierDelegate implementation where the
 * underlying strategy causes the enerated identitifer to be returned as an
 * effect of performing the insert statement.  Thus, there is no need for an
 * additional sql statement to determine the generated identitifer.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractReturningDelegate implements InsertGeneratedIdentifierDelegate {
	private final PostInsertIdentityPersister persister;

	public AbstractReturningDelegate(PostInsertIdentityPersister persister) {
		this.persister = persister;
	}

	public final Serializable performInsert(String insertSQL, SessionImplementor session, Binder binder) {
		try {
			// prepare and execute the insert
			PreparedStatement insert = prepare( insertSQL, session );
			try {
				binder.bindValues( insert );
				return executeAndExtract( insert );
			}
			finally {
				releaseStatement( insert, session );
			}
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
			        sqle,
			        "could not insert: " + MessageHelper.infoString( persister ),
			        insertSQL
				);
		}
	}

	protected PostInsertIdentityPersister getPersister() {
		return persister;
	}

	protected abstract PreparedStatement prepare(String insertSQL, SessionImplementor session) throws SQLException;

	protected abstract Serializable executeAndExtract(PreparedStatement insert) throws SQLException;

	protected void releaseStatement(PreparedStatement insert, SessionImplementor session) throws SQLException {
		session.getBatcher().closeStatement( insert );
	}
}
