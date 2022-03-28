/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.spi.ImplicitIdentifierDatabaseObjectNamingStrategy;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.id.enhanced.TableGenerator.DEF_TABLE;
import static org.hibernate.id.enhanced.TableGenerator.TABLE_PARAM;

public class LegacyNoPreferDefaultGeneratorNameDatabaseNamingStrategy implements
		ImplicitIdentifierDatabaseObjectNamingStrategy {

	public QualifiedName determineSequenceName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

		return new QualifiedSequenceName(
				catalogName,
				schemaName,
				jdbcEnvironment.getIdentifierHelper().toIdentifier( DEF_SEQUENCE )
		);
	}

	public QualifiedName determineTableName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final String tableName = ConfigurationHelper.getString( TABLE_PARAM, configValues, DEF_TABLE );

		if ( tableName.contains( "." ) ) {
			return QualifiedNameParser.INSTANCE.parse( tableName );
		}
		else {
			final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
			return new QualifiedNameParser.NameParts(
					catalogName,
					schemaName,
					jdbcEnvironment.getIdentifierHelper().toIdentifier( tableName )
			);
		}
	}
}
