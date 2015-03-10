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

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;

/**
 * Handle schema generation source from (annotation/xml) metadata.
 *
 * @author Steve Ebersole
 */
public class GenerationSourceFromMetadata implements GenerationSource {
	private final MetadataImplementor metadata;
	private final Dialect dialect;
	private final boolean createAndDropSchemas;
	private final boolean creation;

	public GenerationSourceFromMetadata(
			MetadataImplementor metadata,
			Dialect dialect,
			boolean createAndDropSchemas,
			boolean creation) {
		this.metadata = metadata;
		this.dialect = dialect;
		this.createAndDropSchemas = createAndDropSchemas;
		this.creation = creation;
	}

	@Override
	public Iterable<String> getCommands() {
		if ( creation ) {
			// for now...
			return new SchemaCreatorImpl().generateCreationCommands( metadata, createAndDropSchemas, dialect );
		}
		else {
			return new SchemaDropperImpl().generateDropCommands( metadata, createAndDropSchemas, dialect );
		}
	}

	@Override
	public void release() {
		// nothing to do
	}
}
