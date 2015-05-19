/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.spi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Maintains the set of ANSI SQL keywords
 *
 * @author Steve Ebersole
 */
public class AnsiSqlKeywords {
	/**
	 * Singleton access
	 */
	public static final AnsiSqlKeywords INSTANCE = new AnsiSqlKeywords();

	private final Set<String> keywordsSql2003;

	public AnsiSqlKeywords() {
		final Set<String> keywordsSql2003 = new HashSet<String>();
		keywordsSql2003.add( "ADD" );
		keywordsSql2003.add( "ALL" );
		keywordsSql2003.add( "ALLOCATE" );
		keywordsSql2003.add( "ALTER" );
		keywordsSql2003.add( "AND" );
		keywordsSql2003.add( "ANY" );
		keywordsSql2003.add( "ARE" );
		keywordsSql2003.add( "ARRAY" );
		keywordsSql2003.add( "AS" );
		keywordsSql2003.add( "ASENSITIVE" );
		keywordsSql2003.add( "ASYMMETRIC" );
		keywordsSql2003.add( "AT" );
		keywordsSql2003.add( "ATOMIC" );
		keywordsSql2003.add( "AUTHORIZATION" );
		keywordsSql2003.add( "BEGIN" );
		keywordsSql2003.add( "BETWEEN" );
		keywordsSql2003.add( "BIGINT" );
		keywordsSql2003.add( "BINARY" );
		keywordsSql2003.add( "BLOB" );
		keywordsSql2003.add( "BINARY" );
		keywordsSql2003.add( "BOTH" );
		keywordsSql2003.add( "BY" );
		keywordsSql2003.add( "CALL" );
		keywordsSql2003.add( "CALLED" );
		keywordsSql2003.add( "CASCADED" );
		keywordsSql2003.add( "CASE" );
		keywordsSql2003.add( "CAST" );
		keywordsSql2003.add( "CHAR" );
		keywordsSql2003.add( "CHARACTER" );
		keywordsSql2003.add( "CHECK" );
		keywordsSql2003.add( "CLOB" );
		keywordsSql2003.add( "CLOB" );
		keywordsSql2003.add( "CLOSE" );
		keywordsSql2003.add( "COLLATE" );
		keywordsSql2003.add( "COLUMN" );
		keywordsSql2003.add( "COMMIT" );
		keywordsSql2003.add( "CONDITION" );
		keywordsSql2003.add( "CONNECT" );
		keywordsSql2003.add( "CONSTRAINT" );
		keywordsSql2003.add( "CONTINUE" );
		keywordsSql2003.add( "CORRESPONDING" );
		keywordsSql2003.add( "CREATE" );
		keywordsSql2003.add( "CROSS" );
		keywordsSql2003.add( "CUBE" );
		keywordsSql2003.add( "CURRENT" );
		keywordsSql2003.add( "CURRENT_DATE" );
		keywordsSql2003.add( "CURRENT_PATH" );
		keywordsSql2003.add( "CURRENT_ROLE" );
		keywordsSql2003.add( "CURRENT_TIME" );
		keywordsSql2003.add( "CURRENT_TIMESTAMP" );
		keywordsSql2003.add( "CURRENT_USER" );
		keywordsSql2003.add( "CURSOR" );
		keywordsSql2003.add( "CYCLE" );
		keywordsSql2003.add( "DATE" );
		keywordsSql2003.add( "DAY" );
		keywordsSql2003.add( "DEALLOCATE" );
		keywordsSql2003.add( "DEC" );
		keywordsSql2003.add( "DECIMAL" );
		keywordsSql2003.add( "DECLARE" );
		keywordsSql2003.add( "DEFAULT" );
		keywordsSql2003.add( "DELETE" );
		keywordsSql2003.add( "DEREF" );
		keywordsSql2003.add( "DESCRIBE" );
		keywordsSql2003.add( "DETERMINISTIC" );
		keywordsSql2003.add( "DISCONNECT" );
		keywordsSql2003.add( "DISTINCT" );
		keywordsSql2003.add( "DO" );
		keywordsSql2003.add( "DOUBLE" );
		keywordsSql2003.add( "DROP" );
		keywordsSql2003.add( "DYNAMIC" );
		keywordsSql2003.add( "EACH" );
		keywordsSql2003.add( "ELEMENT" );
		keywordsSql2003.add( "ELSE" );
		keywordsSql2003.add( "ELSIF" );
		keywordsSql2003.add( "END" );
		keywordsSql2003.add( "ESCAPE" );
		keywordsSql2003.add( "EXCEPT" );
		keywordsSql2003.add( "EXEC" );
		keywordsSql2003.add( "EXECUTE" );
		keywordsSql2003.add( "EXISTS" );
		keywordsSql2003.add( "EXIT" );
		keywordsSql2003.add( "EXTERNAL" );
		keywordsSql2003.add( "FALSE" );
		keywordsSql2003.add( "FETCH" );
		keywordsSql2003.add( "FILTER" );
		keywordsSql2003.add( "FLOAT" );
		keywordsSql2003.add( "FOR" );
		keywordsSql2003.add( "FOREIGN" );
		keywordsSql2003.add( "FREE" );
		keywordsSql2003.add( "FROM" );
		keywordsSql2003.add( "FULL" );
		keywordsSql2003.add( "FUNCTION" );
		keywordsSql2003.add( "GET" );
		keywordsSql2003.add( "GLOBAL" );
		keywordsSql2003.add( "GRANT" );
		keywordsSql2003.add( "GROUP" );
		keywordsSql2003.add( "GROUPING" );
		keywordsSql2003.add( "HANDLER" );
		keywordsSql2003.add( "HAVING" );
		keywordsSql2003.add( "HOLD" );
		keywordsSql2003.add( "HOUR" );
		keywordsSql2003.add( "IDENTITY" );
		keywordsSql2003.add( "IF" );
		keywordsSql2003.add( "IMMEDIATE" );
		keywordsSql2003.add( "IN" );
		keywordsSql2003.add( "INDICATOR" );
		keywordsSql2003.add( "INNER" );
		keywordsSql2003.add( "INOUT" );
		keywordsSql2003.add( "INPUT" );
		keywordsSql2003.add( "INSENSITIVE" );
		keywordsSql2003.add( "INSERT" );
		keywordsSql2003.add( "INT" );
		keywordsSql2003.add( "INTEGER" );
		keywordsSql2003.add( "INTERSECT" );
		keywordsSql2003.add( "INTERVAL" );
		keywordsSql2003.add( "INTO" );
		keywordsSql2003.add( "IS" );
		keywordsSql2003.add( "ITERATE" );
		keywordsSql2003.add( "JOIN" );
		keywordsSql2003.add( "LANGUAGE" );
		keywordsSql2003.add( "LARGE" );
		keywordsSql2003.add( "LATERAL" );
		keywordsSql2003.add( "LEADING" );
		keywordsSql2003.add( "LEAVE" );
		keywordsSql2003.add( "LEFT" );
		keywordsSql2003.add( "LIKE" );
		keywordsSql2003.add( "LOCAL" );
		keywordsSql2003.add( "LOCALTIME" );
		keywordsSql2003.add( "LOCALTIMESTAMP" );
		keywordsSql2003.add( "LOOP" );
		keywordsSql2003.add( "MATCH" );
		keywordsSql2003.add( "MEMBER" );
		keywordsSql2003.add( "MERGE" );
		keywordsSql2003.add( "METHOD" );
		keywordsSql2003.add( "MINUTE" );
		keywordsSql2003.add( "MODIFIES" );
		keywordsSql2003.add( "MODULE" );
		keywordsSql2003.add( "MONTH" );
		keywordsSql2003.add( "MULTISET" );
		keywordsSql2003.add( "NATIONAL" );
		keywordsSql2003.add( "NATURAL" );
		keywordsSql2003.add( "NCHAR" );
		keywordsSql2003.add( "NCLOB" );
		keywordsSql2003.add( "NEW" );
		keywordsSql2003.add( "NO" );
		keywordsSql2003.add( "NONE" );
		keywordsSql2003.add( "NOT" );
		keywordsSql2003.add( "NULL" );
		keywordsSql2003.add( "NUMERIC" );
		keywordsSql2003.add( "OF" );
		keywordsSql2003.add( "OLD" );
		keywordsSql2003.add( "ON" );
		keywordsSql2003.add( "ONLY" );
		keywordsSql2003.add( "OPEN" );
		keywordsSql2003.add( "OR" );
		keywordsSql2003.add( "ORDER" );
		keywordsSql2003.add( "OUT" );
		keywordsSql2003.add( "OUTER" );
		keywordsSql2003.add( "OUTPUT" );
		keywordsSql2003.add( "OVER" );
		keywordsSql2003.add( "OVERLAPS" );
		keywordsSql2003.add( "PARAMETER" );
		keywordsSql2003.add( "PARTITION" );
		keywordsSql2003.add( "PRECISION" );
		keywordsSql2003.add( "PREPARE" );
		keywordsSql2003.add( "PRIMARY" );
		keywordsSql2003.add( "PROCEDURE" );
		keywordsSql2003.add( "RANGE" );
		keywordsSql2003.add( "READS" );
		keywordsSql2003.add( "REAL" );
		keywordsSql2003.add( "RECURSIVE" );
		keywordsSql2003.add( "REF" );
		keywordsSql2003.add( "REFERENCES" );
		keywordsSql2003.add( "REFERENCING" );
		keywordsSql2003.add( "RELEASE" );
		keywordsSql2003.add( "REPEAT" );
		keywordsSql2003.add( "RESIGNAL" );
		keywordsSql2003.add( "RESULT" );
		keywordsSql2003.add( "RETURN" );
		keywordsSql2003.add( "RETURNS" );
		keywordsSql2003.add( "REVOKE" );
		keywordsSql2003.add( "RIGHT" );
		keywordsSql2003.add( "ROLLBACK" );
		keywordsSql2003.add( "ROLLUP" );
		keywordsSql2003.add( "ROW" );
		keywordsSql2003.add( "ROWS" );
		keywordsSql2003.add( "SAVEPOINT" );
		keywordsSql2003.add( "SCROLL" );
		keywordsSql2003.add( "SEARCH" );
		keywordsSql2003.add( "SECOND" );
		keywordsSql2003.add( "SELECT" );
		keywordsSql2003.add( "SENSITIVE" );
		keywordsSql2003.add( "SESSION_USE" );
		keywordsSql2003.add( "SET" );
		keywordsSql2003.add( "SIGNAL" );
		keywordsSql2003.add( "SIMILAR" );
		keywordsSql2003.add( "SMALLINT" );
		keywordsSql2003.add( "SOME" );
		keywordsSql2003.add( "SPECIFIC" );
		keywordsSql2003.add( "SPECIFICTYPE" );
		keywordsSql2003.add( "SQL" );
		keywordsSql2003.add( "SQLEXCEPTION" );
		keywordsSql2003.add( "SQLSTATE" );
		keywordsSql2003.add( "SQLWARNING" );
		keywordsSql2003.add( "START" );
		keywordsSql2003.add( "STATIC" );
		keywordsSql2003.add( "SUBMULTISET" );
		keywordsSql2003.add( "SYMMETRIC" );
		keywordsSql2003.add( "SYSTEM" );
		keywordsSql2003.add( "SYSTEM_USER" );
		keywordsSql2003.add( "TABLE" );
		keywordsSql2003.add( "TABLESAMPLE" );
		keywordsSql2003.add( "THEN" );
		keywordsSql2003.add( "TIME" );
		keywordsSql2003.add( "TIMESTAMP" );
		keywordsSql2003.add( "TIMEZONE_HOUR" );
		keywordsSql2003.add( "TIMEZONE_MINUTE" );
		keywordsSql2003.add( "TO" );
		keywordsSql2003.add( "TRAILING" );
		keywordsSql2003.add( "TRANSLATION" );
		keywordsSql2003.add( "TREAT" );
		keywordsSql2003.add( "TRIGGER" );
		keywordsSql2003.add( "TRUE" );
		keywordsSql2003.add( "UNDO" );
		keywordsSql2003.add( "UNION" );
		keywordsSql2003.add( "UNIQUE" );
		keywordsSql2003.add( "UNKNOWN" );
		keywordsSql2003.add( "UNNEST" );
		keywordsSql2003.add( "UNTIL" );
		keywordsSql2003.add( "UPDATE" );
		keywordsSql2003.add( "USER" );
		keywordsSql2003.add( "USING" );
		keywordsSql2003.add( "VALUE" );
		keywordsSql2003.add( "VALUES" );
		keywordsSql2003.add( "VARCHAR" );
		keywordsSql2003.add( "VARYING" );
		keywordsSql2003.add( "WHEN" );
		keywordsSql2003.add( "WHENEVER" );
		keywordsSql2003.add( "WHERE" );
		keywordsSql2003.add( "WHILE" );
		keywordsSql2003.add( "WINDOW" );
		keywordsSql2003.add( "WITH" );
		keywordsSql2003.add( "WITHIN" );
		keywordsSql2003.add( "WITHOUT" );
		keywordsSql2003.add( "YEAR" );

		this.keywordsSql2003 = Collections.unmodifiableSet( keywordsSql2003 );
	}

	/**
	 * Retrieve all keywords defined by ANSI SQL:2003
	 *
	 * @return ANSI SQL:2003 keywords
	 */
	public Set<String> sql2003() {
		return keywordsSql2003;
	}


}
