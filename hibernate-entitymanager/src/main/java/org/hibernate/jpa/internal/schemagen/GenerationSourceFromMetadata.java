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

import java.util.Arrays;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;

/**
 * Handle schema generation source from (annotation/xml) metadata.
 *
 * @author Steve Ebersole
 */
public class GenerationSourceFromMetadata implements GenerationSource {
	private final Configuration hibernateConfiguration;
	private final Dialect dialect;
	private final boolean creation;

	public GenerationSourceFromMetadata(Configuration hibernateConfiguration, Dialect dialect, boolean creation) {
		this.hibernateConfiguration = hibernateConfiguration;
		this.dialect = dialect;
		this.creation = creation;
	}

	@Override
	public Iterable<String> getCommands() {
		return creation
				? Arrays.asList( hibernateConfiguration.generateSchemaCreationScript( dialect ) )
				: Arrays.asList( hibernateConfiguration.generateDropSchemaScript( dialect ) );
	}

	@Override
	public void release() {
		// nothing to do
	}
}
