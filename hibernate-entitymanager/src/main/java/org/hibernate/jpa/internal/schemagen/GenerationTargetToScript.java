/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
