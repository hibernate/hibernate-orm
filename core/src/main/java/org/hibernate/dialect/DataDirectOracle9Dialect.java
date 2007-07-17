package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataDirectOracle9Dialect extends Oracle9Dialect {
	
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col; // sql server just returns automatically
	}
	
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute(); 
//		 This assumes you will want to ignore any update counts 
		while (!isResultSet && ps.getUpdateCount() != -1) { 
		    isResultSet = ps.getMoreResults(); 
		} 
		ResultSet rs = ps.getResultSet(); 
//		 You may still have other ResultSets or update counts left to process here 
//		 but you can't do it now or the ResultSet you just got will be closed 
		return rs;
	}

}
