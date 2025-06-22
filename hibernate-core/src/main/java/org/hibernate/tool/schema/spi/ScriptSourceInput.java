/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	default boolean exists() {
		return true;
	}

}
