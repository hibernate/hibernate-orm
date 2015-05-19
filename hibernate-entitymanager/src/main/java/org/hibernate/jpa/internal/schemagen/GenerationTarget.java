/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

/**
 * Describes a schema generation target
 *
 * @see org.hibernate.jpa.AvailableSettings#SCHEMA_GEN_SCRIPTS_ACTION
 * @see org.hibernate.jpa.AvailableSettings#SCHEMA_GEN_SCRIPTS_CREATE_TARGET
 * @see org.hibernate.jpa.AvailableSettings#SCHEMA_GEN_SCRIPTS_DROP_TARGET
 *
 * @author Steve Ebersole
 */
interface GenerationTarget {
	/**
	 * Accept a group of create generation commands
	 *
	 * @param commands The commands
	 */
	public void acceptCreateCommands(Iterable<String> commands);

	/**
	 * Accept a group of drop generation commands.
	 *
	 * @param commands The commands
	 */
	public void acceptDropCommands(Iterable<String> commands);

	/**
	 * Release this target, giving it a change to release its resources.
	 */
	public void release();
}
