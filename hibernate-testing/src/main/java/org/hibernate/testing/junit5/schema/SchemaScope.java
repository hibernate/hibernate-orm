/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.schema;

import java.util.function.Consumer;

import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaMigrator;

import org.hibernate.testing.junit5.template.TestScope;


/**
 * @author Andrea Boriero
 */
public interface SchemaScope extends TestScope {

	void withSchemaUpdate(Consumer<SchemaUpdate> counsumer);

	void withSchemaValidator(Consumer<SchemaValidator> counsumer);

	void withSchemaMigrator(Consumer<SchemaMigrator> counsumer);

	void withSchemaExport(Consumer<SchemaExport> counsumer);

	void withSchemaCreator(SchemaFilter filter, Consumer<SchemaCreatorImpl> consumer);

	void withSchemaDropper(SchemaFilter filter, Consumer<SchemaDropperImpl> consumer);
}
