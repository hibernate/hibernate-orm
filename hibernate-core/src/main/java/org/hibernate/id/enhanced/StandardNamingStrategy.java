/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.enhanced;

import java.util.Map;

import jakarta.persistence.GeneratedValue;

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
 * Hibernate's standard implicit naming strategy for identifier sequences and tables.
 *
 * For sequences (including forced-table sequences):<ol>
 *     <li>
 *         If {@value SequenceStyleGenerator#CONFIG_SEQUENCE_PER_ENTITY_SUFFIX} is specified,
 *         a name composed of the "base" name with the specified suffix.  The base name
 *         depends on the usage of the generator, but is generally the root entity-name if
 *         applied to an entity identifier or the table we are generating values for
 *     </li>
 *     <li>
 *         If annotations are used and {@link GeneratedValue#generator()} is specified,
 *         its value is used as the sequence name
 *     </li>
 *     <li>
 *         If the "base" name is known, use that
 *     </li>
 *     <li>
 *         Throw an exception
 *     </li>
 * </ol>
 *
 * For tables:<ol>
 *     <li>
 *         If annotations are used and {@link GeneratedValue#generator()} is specified,
 *         its value is used as the table name
 *     </li>
 *     <li>
 *         Fall back is to use {@value TableGenerator#DEF_TABLE}
 *     </li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class StandardNamingStrategy implements ImplicitDatabaseObjectNamingStrategy {
	public static final String STRATEGY_NAME = "standard";

	@Override
	public QualifiedName determineSequenceName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

		final String rootTableName = ConfigurationHelper.getString( PersistentIdentifierGenerator.TABLE, configValues );
		final String implicitName = implicitSequenceName( rootTableName, configValues, serviceRegistry );

		if ( implicitName.contains( "." ) ) {
			return QualifiedNameParser.INSTANCE.parse( implicitName );
		}

		return new QualifiedSequenceName(
				catalogName,
				schemaName,
				jdbcEnvironment.getIdentifierHelper().toIdentifier( implicitName )
		);
	}

	private static String implicitSequenceName(
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
