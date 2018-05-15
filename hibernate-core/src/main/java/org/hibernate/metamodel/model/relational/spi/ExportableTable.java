/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.Collection;
import java.util.List;

import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.naming.QualifiedTableName;
import org.hibernate.naming.Identifier;

/**
 * @author Andrea Boriero
 */
public interface ExportableTable extends Table, Exportable {
	Identifier getCatalogName();

	Identifier getSchemaName();

	Identifier getTableName();

	QualifiedTableName getQualifiedTableName();

	Collection<PhysicalColumn> getPhysicalColumns();

	String getComment();

	Collection<UniqueKey> getUniqueKeys();

	List<String> getCheckConstraints();

	Collection<Index> getIndexes();

	boolean isPrimaryKeyIdentity();

	Collection<InitCommand> getInitCommands();
}
