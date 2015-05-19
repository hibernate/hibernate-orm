/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
