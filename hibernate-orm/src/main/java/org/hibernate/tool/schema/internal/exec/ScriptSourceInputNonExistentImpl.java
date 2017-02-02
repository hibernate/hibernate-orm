/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.util.Collections;
import java.util.List;

import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;
import org.hibernate.tool.schema.spi.ScriptSourceInput;

/**
 * Used in cases where a specified source cannot be found
 *
 * @author Steve Ebersole
 */
public class ScriptSourceInputNonExistentImpl implements ScriptSourceInput {
	/**
	 * Singleton access
	 */
	public static final ScriptSourceInputNonExistentImpl INSTANCE = new ScriptSourceInputNonExistentImpl();

	@Override
	public void prepare() {
	}

	@Override
	public List<String> read(ImportSqlCommandExtractor commandExtractor) {
		return Collections.emptyList();
	}

	@Override
	public void release() {
	}
}
