//$Id: DDLFormatter.java 7664 2005-07-27 23:29:37Z oneovthafew $
package org.hibernate.pretty;

import java.util.StringTokenizer;

public class DDLFormatter {
	
	private String sql;
	
	public DDLFormatter(String sql) {
		this.sql = sql;
	}

	/**
	 * Format an SQL statement using simple rules:
	 *  a) Insert newline after each comma;
	 *  b) Indent three spaces after each inserted newline;
	 * If the statement contains single/double quotes return unchanged,
	 * it is too complex and could be broken by simple formatting.
	 */
	public String format() {
		if ( sql.toLowerCase().startsWith("create table") ) {
			return formatCreateTable();
		}
		else if ( sql.toLowerCase().startsWith("alter table") ) {
			return formatAlterTable();
		}
		else if ( sql.toLowerCase().startsWith("comment on") ) {
			return formatCommentOn();
		}
		else {
			return "\n    " + sql;
		}
	}

	private String formatCommentOn() {
		StringBuffer result = new StringBuffer(60).append("\n    ");
		StringTokenizer tokens = new StringTokenizer( sql, " '[]\"", true );
	
		boolean quoted = false;
		while ( tokens.hasMoreTokens() ) {
			String token = tokens.nextToken();
			result.append(token);
			if ( isQuote(token) ) {
				quoted = !quoted;
			}
			else if (!quoted) {
				if ( "is".equals(token) ) {
					result.append("\n       ");
				}
			}
		}
		
		return result.toString();
	}

	private String formatAlterTable() {
		StringBuffer result = new StringBuffer(60).append("\n    ");
		StringTokenizer tokens = new StringTokenizer( sql, " (,)'[]\"", true );
	
		boolean quoted = false;
		while ( tokens.hasMoreTokens() ) {
			String token = tokens.nextToken();
			if ( isQuote(token) ) {
				quoted = !quoted;
			}
			else if (!quoted) {
				if ( isBreak(token) ) {
					result.append("\n        ");
				}
			}
			result.append(token);
		}
		
		return result.toString();
	}

	private String formatCreateTable() {
		StringBuffer result = new StringBuffer(60).append("\n    ");
		StringTokenizer tokens = new StringTokenizer( sql, "(,)'[]\"", true );
	
		int depth = 0;
		boolean quoted = false;
		while ( tokens.hasMoreTokens() ) {
			String token = tokens.nextToken();
			if ( isQuote(token) ) {
				quoted = !quoted;
				result.append(token);
			}
			else if (quoted) {
				result.append(token);
			}
			else {
				if ( ")".equals(token) ) {
					depth--;
					if (depth==0) result.append("\n    ");
				}
				result.append(token);
				if ( ",".equals(token) && depth==1 ) result.append("\n       ");
				if ( "(".equals(token) ) {
					depth++;
					if (depth==1) result.append("\n        ");
				}
			}
		}
		
		return result.toString();
	}

	private static boolean isBreak(String token) {
		return "drop".equals(token) ||
			"add".equals(token) || 
			"references".equals(token) || 
			"foreign".equals(token) ||
			"on".equals(token);
	}

	private static boolean isQuote(String tok) {
		return "\"".equals(tok) || 
				"`".equals(tok) || 
				"]".equals(tok) || 
				"[".equals(tok) ||
				"'".equals(tok);
	}

}
