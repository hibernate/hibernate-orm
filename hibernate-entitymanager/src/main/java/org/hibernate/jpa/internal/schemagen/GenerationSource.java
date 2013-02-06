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
