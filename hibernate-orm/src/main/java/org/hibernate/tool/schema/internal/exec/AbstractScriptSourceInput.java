/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;
import org.hibernate.tool.schema.spi.ScriptSourceInput;

/**
 * Convenience base class for ScriptSourceInput implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractScriptSourceInput implements ScriptSourceInput {
	protected abstract Reader reader();

	@Override
	public void prepare() {
		// by default there is nothing to do
	}

	@Override
	public List<String> read(ImportSqlCommandExtractor commandExtractor) {
		final String[] commands = commandExtractor.extractCommands( reader() );
		if ( commands == null ) {
			return Collections.emptyList();
		}
		else {
			return Arrays.asList( commands );
		}
	}

	@Override
	public void release() {
		// by default there is nothing to do
	}
}
