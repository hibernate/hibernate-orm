/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;

/**
 * Handles schema generation source from a "script"
 *
 * @author Steve Ebersole
 */
public class GenerationSourceFromScript implements GenerationSource {
	private final ScriptSourceInput inputSource;
	private final ImportSqlCommandExtractor scriptCommandExtractor;

	public GenerationSourceFromScript(ScriptSourceInput inputSource, ImportSqlCommandExtractor scriptCommandExtractor) {
		this.inputSource = inputSource;
		this.scriptCommandExtractor = scriptCommandExtractor;
	}

	@Override
	public Iterable<String> getCommands() {
		return inputSource.read( scriptCommandExtractor );
	}

	@Override
	public void release() {
		inputSource.release();
	}

}
