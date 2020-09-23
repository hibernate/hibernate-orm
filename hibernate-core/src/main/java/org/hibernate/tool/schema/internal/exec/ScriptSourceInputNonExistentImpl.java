/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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
	public List<String> extract(Function<Reader, List<String>> extractor) {
		return Collections.emptyList();
	}
}
