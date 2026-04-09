/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * Iterator over a resultset; intended usage only for metadata reading.  
 */
public abstract class ResultSetIterator implements Iterator<Map<String, Object>> {

	private ResultSet rs;

	protected boolean current = false;

	protected boolean endOfRows = false;

	private Statement statement = null;

	protected ResultSetIterator(ResultSet resultset) {
		this(null, resultset);
	}

	public ResultSetIterator(Statement stmt, ResultSet resultset) {
		this.rs = resultset;
		this.statement  = stmt;		
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
