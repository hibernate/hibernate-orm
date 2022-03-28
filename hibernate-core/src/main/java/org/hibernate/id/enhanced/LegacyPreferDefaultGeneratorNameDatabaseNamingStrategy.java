/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.util.Map;
import java.util.Objects;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.spi.ImplicitIdentifierDatabaseObjectNamingStrategy;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.id.enhanced.SequenceStyleGenerator.CONFIG_SEQUENCE_PER_ENTITY_SUFFIX;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX;
import static org.hibernate.id.enhanced.TableGenerator.DEF_TABLE;
import static org.hibernate.id.enhanced.TableGenerator.TABLE_PARAM;

public class LegacyPreferDefaultGeneratorNameDatabaseNamingStrategy implements
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
				jdbcEnvironment.getIdentifierHelper().toIdentifier( implicitName( configValues ) )
		);
	}

	private String implicitName(Map<?, ?> configValues) {
		final String suffix = ConfigurationHelper.getString(
				CONFIG_SEQUENCE_PER_ENTITY_SUFFIX,
				configValues,
				DEF_SEQUENCE_SUFFIX
		);

		if ( !Objects.equals( suffix, DEF_SEQUENCE_SUFFIX ) ) {
			return DEF_SEQUENCE;
		}

		final String annotationGeneratorName = ConfigurationHelper.getString(
				IdentifierGenerator.GENERATOR_NAME,
				configValues
		);
		if ( StringHelper.isNotEmpty( annotationGeneratorName ) ) {
			return annotationGeneratorName;
		}

		return DEF_SEQUENCE;
	}

	@Override
	public QualifiedName determineTableName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		String fallbackTableName = DEF_TABLE;

		final String generatorName = ConfigurationHelper.getString( IdentifierGenerator.GENERATOR_NAME, configValues );
		if ( StringHelper.isNotEmpty( generatorName ) ) {
			fallbackTableName = generatorName;
		}

		final String tableName = ConfigurationHelper.getString( TABLE_PARAM, configValues, fallbackTableName );

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
