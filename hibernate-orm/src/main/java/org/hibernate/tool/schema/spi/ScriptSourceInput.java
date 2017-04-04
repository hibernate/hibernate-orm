/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.util.List;

import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;

/**
 * Contract for hiding the differences between a passed Reader, File or URL in terms of how we read input
 * scripts.
 *
 * @author Steve Ebersole
 */
public interface ScriptSourceInput {
	/**
	 * Prepare source for use.
	 */
	void prepare();

	/**
	 * Read the abstracted script, using the given extractor to split up the input into individual commands.
	 *
	 * @param commandExtractor The extractor for individual commands within the input.
	 *
	 * @return The scripted commands
	 */
	List<String> read(ImportSqlCommandExtractor commandExtractor);

	/**
	 * Release this input.
	 */
	void release();
}
