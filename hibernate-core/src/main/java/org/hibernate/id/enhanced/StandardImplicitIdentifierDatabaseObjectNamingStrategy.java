/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.enhanced;

import java.util.Map;
import java.util.Objects;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.spi.ImplicitIdentifierDatabaseObjectNamingStrategy;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.id.OptimizableGenerator.IMPLICIT_NAME_BASE;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.CONFIG_SEQUENCE_PER_ENTITY_SUFFIX;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX;

/**
 * @author Steve Ebersole
 */
public class StandardImplicitIdentifierDatabaseObjectNamingStrategy implements ImplicitIdentifierDatabaseObjectNamingStrategy {
	@Override
	public QualifiedName determineSequenceName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

		final String rootTableName = ConfigurationHelper.getString( PersistentIdentifierGenerator.TABLE, configValues );
		final String implicitName = implicitName( rootTableName, configValues, serviceRegistry );

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
		final String annotationGeneratorName = ConfigurationHelper.getString( IdentifierGenerator.GENERATOR_NAME, configValues );
		final String base = ConfigurationHelper.getString( IMPLICIT_NAME_BASE, configValues );
		final String suffix = ConfigurationHelper.getString( CONFIG_SEQUENCE_PER_ENTITY_SUFFIX, configValues, DEF_SEQUENCE_SUFFIX );

		if ( ! Objects.equals( suffix, DEF_SEQUENCE_SUFFIX ) ) {
			// an "implicit name suffix" was specified
			if ( StringHelper.isNotEmpty( base ) ) {
				if ( Identifier.isQuoted( base ) ) {
					return "`" + Identifier.unQuote( base ) + suffix + "`";
				}
				return base + suffix;
			}
		}

		if ( StringHelper.isNotEmpty( annotationGeneratorName ) ) {
			return annotationGeneratorName;
		}

		if ( StringHelper.isNotEmpty( base ) ) {
			if ( Identifier.isQuoted( base ) ) {
				return "`" + Identifier.unQuote( base ) + suffix + "`";
			}
			return base + suffix;
		}

		throw new MappingException( "Unable to determine implicit sequence name; target table - " + rootTableName );
	}


	@Override
	public QualifiedName determineTableName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

		final String rootTableName = ConfigurationHelper.getString( PersistentIdentifierGenerator.TABLE, configValues );
		final String implicitName = implicitName( rootTableName, configValues, serviceRegistry );

		return new QualifiedTableName(
				catalogName,
				schemaName,
				jdbcEnvironment.getIdentifierHelper().toIdentifier( implicitName )
		);
	}
}
