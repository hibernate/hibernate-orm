/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.internal.util.StringHelper;

/**
 * Class responsible for extracting SQL statements from import script. Treats each line as a complete SQL statement.
 * Comment lines shall start with {@code --}, {@code //} or {@code /*} character sequence.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class SingleLineSqlCommandExtractor implements ImportSqlCommandExtractor {
	@Override
	public String[] extractCommands(Reader reader) {
		BufferedReader bufferedReader = new BufferedReader( reader );
		List<String> statementList = new LinkedList<String>();
		try {
			for ( String sql = bufferedReader.readLine(); sql != null; sql = bufferedReader.readLine() ) {
				String trimmedSql = sql.trim();
				if ( trimmedSql.isEmpty() || isComment( trimmedSql ) ) {
					continue;
				}
				if ( trimmedSql.endsWith( ";" ) ) {
					trimmedSql = trimmedSql.substring( 0, trimmedSql.length() - 1 );
				}
				statementList.add( trimmedSql );
			}
			return statementList.toArray( new String[statementList.size()] );
		}
		catch ( IOException e ) {
			throw new ImportScriptException( "Error during import script parsing.", e );
		}
	}

	private boolean isComment(final String line) {
		return line.startsWith( "--" ) || line.startsWith( "//" ) || line.startsWith( "/*" );
	}
}
