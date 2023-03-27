/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.function.Function;

/**
 * Contract for hiding the differences between a passed Reader, File or URL in terms of how we read input
 * scripts.
 *
 * @author Steve Ebersole
 */
public interface ScriptSourceInput {

	default String getScriptDescription() {
		return toString();
	}

	/**
	 * Allows managed access to the input's Reader, returning a result
	 */
	List<String> extract(Function<Reader, List<String>> extractor);

	default boolean containsScript(URL url) {
		return false;
	}

}
