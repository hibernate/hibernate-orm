/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.SQLException;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * Will cache all database information by reading the tableinformation from the name space in one call.
 * Schema migration becomes much faster when this object is used.
 * NOTE: superclass already caches the sequence information, so perhaps cache can also be coded in superclass
 * @author francois
 */
public class DatabaseInformationCachedImpl extends DatabaseInformationImpl {
	private NameSpaceTablesInformation defaultNameSpaceTablesInformation;
	private final Namespace defaultNamespace;

	/**
	 *
	 * @param serviceRegistry        ServiceRegistry
	 * @param jdbcEnvironment        JdbcEnvironment
	 * @param ddlTransactionIsolator DdlTransactionIsolator
	 * @param defaultNamespace       NameSpace
	 */
	public DatabaseInformationCachedImpl(
			ServiceRegistry serviceRegistry,
			JdbcEnvironment jdbcEnvironment,
			DdlTransactionIsolator ddlTransactionIsolator, Namespace defaultNamespace)
			throws SQLException {
		super(serviceRegistry, jdbcEnvironment, ddlTransactionIsolator, defaultNamespace.getName());
		this.defaultNamespace = defaultNamespace;
	}

	@Override
	public TableInformation getTableInformation(QualifiedTableName qualifiedTableName) {
		if (defaultNameSpaceTablesInformation == null) {
			// Load table information from the whole namespace in one go (expected to be used in case of schemaMigration, so (almost) all
			// tables are likely to be retrieved)
			defaultNameSpaceTablesInformation = getTablesInformation(defaultNamespace);
		}

		TableInformation tableInformation = defaultNameSpaceTablesInformation.getTableInformation(qualifiedTableName.getTableName().getText());
		if (tableInformation != null) {
			return tableInformation;
		}
		return super.getTableInformation(qualifiedTableName);  // result of this call can of course also be cached, does not seem to be in scope now
	}

}
