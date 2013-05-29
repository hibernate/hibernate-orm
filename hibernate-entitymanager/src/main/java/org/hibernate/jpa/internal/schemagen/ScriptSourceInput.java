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

import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;

/**
 * Contract for hiding the differences between a passed Reader, File or URL in terms of how we read input
 * scripts.
 *
 * @author Steve Ebersole
 */
public interface ScriptSourceInput {
	/**
	 * Read the abstracted script, using the given extractor to split up the input into individual commands.
	 *
	 * @param commandExtractor The extractor for individual commands within the input.
	 *
	 * @return The scripted commands
	 */
	public Iterable<String> read(ImportSqlCommandExtractor commandExtractor);

	/**
	 * Release this input.
	 */
	public void release();
}
