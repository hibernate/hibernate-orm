/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.Reader;
import java.util.List;

import org.hibernate.tool.schema.ast.SqlScriptParser;

/**
 * Class responsible for extracting SQL statements from import script. Supports instructions/comments and quoted
 * strings spread over multiple lines. Each statement must end with semicolon.
 * 
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Steve Ebersole
 */
public class MultipleLinesSqlCommandExtractor implements ImportSqlCommandExtractor {
	@Override
	public String[] extractCommands(Reader reader) {
		final List<String> commands = SqlScriptParser.extractCommands( reader );

		return commands.toArray( new String[0] );
	}
}
