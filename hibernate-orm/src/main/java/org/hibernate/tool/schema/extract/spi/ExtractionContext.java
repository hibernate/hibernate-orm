/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.spi;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;

/**
 * Defines a context for performing extraction including providing access to information about ongoing extraction as
 * well as to delegates needed in performing extraction.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ExtractionContext {
	ServiceRegistry getServiceRegistry();
	JdbcEnvironment getJdbcEnvironment();
	Connection getJdbcConnection();
	DatabaseMetaData getJdbcDatabaseMetaData();

	Identifier getDefaultCatalog();
	Identifier getDefaultSchema();

	/**
	 * In conjunction with {@link #getDatabaseObjectAccess()} provides access to
	 * information about known database objects to the extractor.
	 */
	@Incubating
	interface DatabaseObjectAccess {
		TableInformation locateTableInformation(QualifiedTableName tableName);
		SequenceInformation locateSequenceInformation(QualifiedSequenceName sequenceName);
	}

	DatabaseObjectAccess getDatabaseObjectAccess();

	void cleanup();
}
