/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;

/**
 * GenerationTarget implementation for handling generation to scripts
 *
 * @author Steve Ebersole
 */
public class GenerationTargetToScript implements GenerationTarget {

	private final ScriptTargetOutput scriptTarget;
	private final String delimiter;

	public GenerationTargetToScript(
			ScriptTargetOutput scriptTarget,
			String delimiter) {
		if ( scriptTarget == null ) {
			throw new SchemaManagementException( "ScriptTargetOutput cannot be null" );
		}
		this.scriptTarget = scriptTarget;
		this.delimiter = delimiter;
	}

	@Override
	public void prepare() {
		scriptTarget.prepare();
	}

	@Override
	public void accept(String command) {
		if ( delimiter != null ) {
			command += delimiter;
		}
		scriptTarget.accept( command );
	}

	@Override
	public void release() {
		scriptTarget.release();
	}

}
