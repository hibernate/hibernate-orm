/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;

/**
 * Service delegate for handling schema creation.
 * <p/>
 * The actual contract here is kind of convoluted with the design
 * idea of allowing this to work in ORM (JDBC) as well as in non-JDBC
 * environments (OGM, e.g.) simultaneously.  ExecutionContext allows
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SchemaCreator {
	/**
	 * Perform a schema creation from the indicated source(s) to the indicated target(s).
	 *
	 * @param metadata Represents the schema to be created.
	 * @param options Options for executing the creation
	 * @param sourceDescriptor description of the source(s) of creation commands
	 * @param targetDescriptor description of the target(s) for the creation commands
	 */
	void doCreation(Metadata metadata, ExecutionOptions options, SourceDescriptor sourceDescriptor, TargetDescriptor targetDescriptor);
}
