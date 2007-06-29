//$Id: GUIDGenerator.java 7265 2005-06-22 04:19:34Z oneovthafew $
package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;

/**
 * Generates <tt>string</tt> values using the SQL Server NEWID() function.
 *
 * @author Joseph Fifield
 */
public class GUIDGenerator implements IdentifierGenerator {

	private static final Log log = LogFactory.getLog(GUIDGenerator.class);

	public Serializable generate(SessionImplementor session, Object obj) 
	throws HibernateException {
		
		final String sql = session.getFactory().getDialect().getSelectGUIDString();
		try {
			PreparedStatement st = session.getBatcher().prepareSelectStatement(sql);
			try {
				ResultSet rs = st.executeQuery();
				final String result;
				try {
					rs.next();
					result = rs.getString(1);
				}
				finally {
					rs.close();
				}
				log.debug("GUID identifier generated: " + result);
				return result;
			}
			finally {
				session.getBatcher().closeStatement(st);
			}
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not retrieve GUID",
					sql
				);
		}
	}

}
