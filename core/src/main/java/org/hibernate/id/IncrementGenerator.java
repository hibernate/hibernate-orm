package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.mapping.Table;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * <b>increment</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that returns a <tt>long</tt>, constructed by
 * counting from the maximum primary key value at startup. Not safe for use in a
 * cluster!<br>
 * <br>
 * Mapping parameters supported, but not usually needed: tables, column.
 * (The tables parameter specified a comma-separated list of table names.)
 *
 * @author Gavin King
 */
public class IncrementGenerator implements IdentifierGenerator, Configurable {

	private static final Log log = LogFactory.getLog(IncrementGenerator.class);

	private long next;
	private String sql;
	private Class returnClass;

	public synchronized Serializable generate(SessionImplementor session, Object object) 
	throws HibernateException {

		if (sql!=null) {
			getNext( session );
		}
		return IdentifierGeneratorFactory.createNumber(next++, returnClass);
	}

	public void configure(Type type, Properties params, Dialect dialect)
	throws MappingException {

		String tableList = params.getProperty("tables");
		if (tableList==null) tableList = params.getProperty(PersistentIdentifierGenerator.TABLES);
		String[] tables = StringHelper.split(", ", tableList);
		String column = params.getProperty("column");
		if (column==null) column = params.getProperty(PersistentIdentifierGenerator.PK);
		String schema = params.getProperty(PersistentIdentifierGenerator.SCHEMA);
		String catalog = params.getProperty(PersistentIdentifierGenerator.CATALOG);
		returnClass = type.getReturnedClass();
		

		StringBuffer buf = new StringBuffer();
		for ( int i=0; i<tables.length; i++ ) {
			if (tables.length>1) {
				buf.append("select ").append(column).append(" from ");
			}
			buf.append( Table.qualify( catalog, schema, tables[i] ) );
			if ( i<tables.length-1) buf.append(" union ");
		}
		if (tables.length>1) {
			buf.insert(0, "( ").append(" ) ids_");
			column = "ids_." + column;
		}
		
		sql = "select max(" + column + ") from " + buf.toString();
	}

	private void getNext( SessionImplementor session ) {

		log.debug("fetching initial value: " + sql);
		
		try {
			PreparedStatement st = session.getBatcher().prepareSelectStatement(sql);
			try {
				ResultSet rs = st.executeQuery();
				try {
					if ( rs.next() ) {
						next = rs.getLong(1) + 1;
						if ( rs.wasNull() ) next = 1;
					}
					else {
						next = 1;
					}
					sql=null;
					log.debug("first free id: " + next);
				}
				finally {
					rs.close();
				}
			}
			finally {
				session.getBatcher().closeStatement(st);
			}
			
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not fetch initial value for increment generator",
					sql
				);
		}
	}

}
