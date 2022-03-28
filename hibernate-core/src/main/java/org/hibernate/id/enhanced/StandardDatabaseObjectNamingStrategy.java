/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.enhanced;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.id.OptimizableGenerator.IMPLICIT_NAME_BASE;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.CONFIG_SEQUENCE_PER_ENTITY_SUFFIX;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX;
import static org.hibernate.id.enhanced.TableGenerator.DEF_TABLE;

/**
 * @author Steve Ebersole
 */
public class StandardDatabaseObjectNamingStrategy implements ImplicitDatabaseObjectNamingStrategy {
	public static final String STRATEGY_NAME = "default";

	@Override
	public QualifiedName determineSequenceName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

		final String rootTableName = ConfigurationHelper.getString( PersistentIdentifierGenerator.TABLE, configValues );
		final String implicitName = implicitName( rootTableName, configValues, serviceRegistry );

		if ( implicitName.contains( "." ) ) {
			return QualifiedNameParser.INSTANCE.parse( implicitName );
		}

		return new QualifiedSequenceName(
				catalogName,
				schemaName,
				jdbcEnvironment.getIdentifierHelper().toIdentifier( implicitName )
		);
	}

	private static String implicitName(
			String rootTableName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final String explicitSuffix = ConfigurationHelper.getString( CONFIG_SEQUENCE_PER_ENTITY_SUFFIX, configValues );
		final String base = ConfigurationHelper.getString( IMPLICIT_NAME_BASE, configValues, rootTableName );

		if ( StringHelper.isNotEmpty( explicitSuffix ) ) {
			// an "implicit name suffix" was specified
			if ( StringHelper.isNotEmpty( base ) ) {
				if ( Identifier.isQuoted( base ) ) {
					return "`" + Identifier.unQuote( base ) + explicitSuffix + "`";
				}
				return base + explicitSuffix;
			}
		}

		final String annotationGeneratorName = ConfigurationHelper.getString( IdentifierGenerator.GENERATOR_NAME, configValues );
		if ( StringHelper.isNotEmpty( annotationGeneratorName ) ) {
			return annotationGeneratorName;
		}

		if ( StringHelper.isNotEmpty( base ) ) {
			if ( Identifier.isQuoted( base ) ) {
				return "`" + Identifier.unQuote( base ) + DEF_SEQUENCE_SUFFIX + "`";
			}
			return base + DEF_SEQUENCE_SUFFIX;
		}

		throw new MappingException( "Unable to determine implicit sequence name; target table - " + rootTableName );
	}

	@Override
	public QualifiedName determineTableName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final String tableName = implicitTableName( configValues );

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

	private static String implicitTableName(Map<?, ?> configValues) {
		final String generatorName = ConfigurationHelper.getString( IdentifierGenerator.GENERATOR_NAME, configValues );
		if ( StringHelper.isNotEmpty( generatorName ) ) {
			return generatorName;
		}

		return DEF_TABLE;
	}

}
