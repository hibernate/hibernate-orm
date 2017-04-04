/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

/**
 * GenerationTarget implementation for handling generation to System.out
 *
 * @author Steve Ebersole
 */
public class GenerationTargetToStdout implements GenerationTarget {
	private final String delimiter;

	public GenerationTargetToStdout(String delimiter) {
		this.delimiter = delimiter;
	}

	public GenerationTargetToStdout() {
		this ( null );
	}

	@Override
	public void prepare() {
		// nothing to do
	}

	@Override
	public void accept(String command) {
		if ( delimiter != null ) {
			command += delimiter;
		}
		System.out.println( command );
	}

	@Override
	public void release() {
	}

}
