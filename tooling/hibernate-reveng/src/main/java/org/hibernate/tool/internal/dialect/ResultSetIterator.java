package org.hibernate.tool.internal.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.hibernate.exception.spi.SQLExceptionConverter;


/**
 * Iterator over a resultset; intended usage only for metadata reading.  
 */
public abstract class ResultSetIterator implements Iterator<Map<String, Object>> {

	private ResultSet rs;

	protected boolean current = false;

	protected boolean endOfRows = false;

	private SQLExceptionConverter sec;

	private Statement statement = null;

	protected ResultSetIterator(ResultSet resultset, SQLExceptionConverter sec) {
		this(null, resultset, sec);
	}

	public ResultSetIterator(Statement stmt, ResultSet resultset, SQLExceptionConverter exceptionConverter) {
		this.rs = resultset;
		this.sec = exceptionConverter;
		this.statement  = stmt;		
	}

	protected SQLExceptionConverter getSQLExceptionConverter() {
		return sec;
	}
	
	public boolean hasNext() {
		try {
			advance();
			return !endOfRows;
		}
		catch (SQLException e) {
			handleSQLException( e );
			return false;
		}
	}

	
	public Map<String, Object> next() {
		try {
			advance();
			if ( endOfRows ) {
				throw new NoSuchElementException();
			}
			current = false;
			return convertRow( rs );
		}
		catch (SQLException e) {
			handleSQLException(e);
			throw new NoSuchElementException("excpetion occurred " + e);
		}

	}

	abstract protected Throwable handleSQLException(SQLException e);
	abstract protected Map<String, Object> convertRow(ResultSet rs) throws SQLException;

	public void remove() {
		throw new UnsupportedOperationException(
				"remove() not possible on ResultSet" );
	}

	protected void advance() throws SQLException {

		if ( !current && !endOfRows ) {
			if ( rs.next() ) {
				current = true;
				endOfRows = false;
			}
			else {
				current = false;
				endOfRows = true;
			}
		}
	}

	public void close() {
		try {
			rs.close();
			if(statement!=null) {
				statement.close();
			}			
		}
		catch (SQLException e) {
			handleSQLException(e);			
		}
	}
}
