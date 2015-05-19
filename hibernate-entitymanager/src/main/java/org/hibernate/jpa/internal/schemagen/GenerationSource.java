/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

/**
 * Contract describing a source for create/drop commands.
 *
 * @see org.hibernate.jpa.SchemaGenSource
 * @see org.hibernate.jpa.AvailableSettings#SCHEMA_GEN_CREATE_SOURCE
 * @see org.hibernate.jpa.AvailableSettings#SCHEMA_GEN_DROP_SOURCE
 *
 * @author Steve Ebersole
 */
interface GenerationSource {
	/**
	 * Retrieve the create generation commands from this source.
	 *
	 * @return The generation commands
	 */
	public Iterable<String> getCommands();

	/**
	 * Release this source.  Give it a change to release its resources, if any.
	 */
	public void release();
}
