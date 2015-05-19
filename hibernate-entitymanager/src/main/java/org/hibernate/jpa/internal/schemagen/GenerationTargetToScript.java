/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

import org.hibernate.jpa.SchemaGenAction;

/**
 * GenerationTarget implementation for handling generation to scripts
 *
 * @author Steve Ebersole
 */
class GenerationTargetToScript implements GenerationTarget {
	private final ScriptTargetOutput createScriptTarget;
	private final ScriptTargetOutput dropScriptTarget;
	private final SchemaGenAction scriptsAction;

	public GenerationTargetToScript(
			ScriptTargetOutput createScriptTarget,
			ScriptTargetOutput dropScriptTarget,
			SchemaGenAction scriptsAction) {
		this.createScriptTarget = createScriptTarget;
		this.dropScriptTarget = dropScriptTarget;
		this.scriptsAction = scriptsAction;
	}

	@Override
	public void acceptCreateCommands(Iterable<String> commands) {
		if ( ! scriptsAction.includesCreate() ) {
			return;
		}

		for ( String command : commands ) {
			createScriptTarget.accept( command );
		}
	}

	@Override
	public void acceptDropCommands(Iterable<String> commands) {
		if ( ! scriptsAction.includesDrop() ) {
			return;
		}

		for ( String command : commands ) {
			dropScriptTarget.accept( command );
		}
	}

	@Override
	public void release() {
		createScriptTarget.release();
		dropScriptTarget.release();
	}

}
