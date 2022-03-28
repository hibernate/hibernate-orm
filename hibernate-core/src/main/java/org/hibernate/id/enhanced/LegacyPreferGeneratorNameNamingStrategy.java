/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.util.Map;

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
import static org.hibernate.id.enhanced.TableGenerator.DEF_TABLE;

/**
 * Naming strategy which prefers, for sequences<ol>
 *     <li>{@link org.hibernate.id.enhanced.TableGenerator#TABLE_PARAM}</li>
 * </ol>
 *
 *
 *
 * falling back to {@value DEF_SEQUENCE}
 */
public class LegacyPreferGeneratorNameNamingStrategy extends LegacyNamingStrategy {
	public static final String STRATEGY_NAME = "prefer-generator-name";

	public QualifiedName determineSequenceName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final String sequenceName = implicitSequenceName( configValues );

		if ( sequenceName.contains( "." ) ) {
			return QualifiedNameParser.INSTANCE.parse( sequenceName );
		}

		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		return new QualifiedSequenceName(
				catalogName,
				schemaName,
				jdbcEnvironment.getIdentifierHelper().toIdentifier( sequenceName )
		);
	}

	private String implicitSequenceName(Map<?, ?> configValues) {
		final String explicitSuffix = ConfigurationHelper.getString( CONFIG_SEQUENCE_PER_ENTITY_SUFFIX, configValues );
		final String rootTableName = ConfigurationHelper.getString( PersistentIdentifierGenerator.TABLE, configValues );
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

		return DEF_SEQUENCE;
	}

	@Override
	public QualifiedName determineTableName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final String implicitName = determineImplicitName( configValues );

		if ( implicitName.contains( "." ) ) {
			return QualifiedNameParser.INSTANCE.parse( implicitName );
		}

		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		return new QualifiedNameParser.NameParts(
				catalogName,
				schemaName,
				jdbcEnvironment.getIdentifierHelper().toIdentifier( implicitName )
		);
	}

	private String determineImplicitName(Map<?, ?> configValues) {
		final String annotationName = ConfigurationHelper.getString( IdentifierGenerator.GENERATOR_NAME, configValues );
		if ( StringHelper.isNotEmpty( annotationName ) ) {
			return annotationName;
		}

		return DEF_TABLE;
	}
}
