/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

/**
 * Contract for hiding the differences between a passed Writer, File or URL in terms of how we write output
 * scripts.
 *
 * @author Steve Ebersole
 */
public interface ScriptTargetOutput {
	/**
	 * Accept the given command and write it to the abstracted script
	 *
	 * @param command The command
	 */
	public void accept(String command);

	/**
	 * Release this output
	 */
	public void release();
}
