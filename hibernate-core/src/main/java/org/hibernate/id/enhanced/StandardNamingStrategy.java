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
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.boot.model.naming.Identifier.isQuoted;
import static org.hibernate.boot.model.naming.Identifier.unQuote;
import static org.hibernate.id.IdentifierGenerator.GENERATOR_NAME;
import static org.hibernate.id.OptimizableGenerator.IMPLICIT_NAME_BASE;
import static org.hibernate.id.PersistentIdentifierGenerator.TABLE;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.CONFIG_SEQUENCE_PER_ENTITY_SUFFIX;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX;
import static org.hibernate.id.enhanced.TableGenerator.DEF_TABLE;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

/**
 * The standard {@linkplain ImplicitDatabaseObjectNamingStrategy implicit naming strategy}
 * for identifier sequences and tables.
 * <p>
 * For sequences (including forced-table sequences):
 * <ol>
 *     <li>
 *         If {@value SequenceStyleGenerator#CONFIG_SEQUENCE_PER_ENTITY_SUFFIX} is specified,
 *         a name is composed of the "base" name with the specified suffix. The base name
 *         depends on the usage of the generator, but is usually the root entity name if
 *         applied to an entity identifier or the table we are generating values for, or
 *     </li>
 *     <li>
 *         If annotations are used and {@link GeneratedValue#generator} is specified,
 *         its value is used as the sequence name.
 *     </li>
 *     <li>
 *         If the "base" name is known, use that.
 *     </li>
 *     <li>
 *         Otherwise, throw a {@link MappingException}.
 *     </li>
 * </ol>
 *<p>
 * For tables:
 * <ol>
 *     <li>
 *         If annotations are used and {@link GeneratedValue#generator} is specified,
 *         its value is used as the table name.
 *     </li>
 *     <li>
 *         Fall back is to use {@value TableGenerator#DEF_TABLE}.
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

		final String rootTableName = getString( TABLE, configValues );
		final String implicitName = implicitSequenceName( rootTableName, configValues, serviceRegistry );

		if ( implicitName.contains( "." ) ) {
			return QualifiedNameParser.INSTANCE.parse( implicitName );
		}

		return new QualifiedSequenceName(
				catalogName,
				schemaName,
				serviceRegistry.requireService( JdbcEnvironment.class )
						.getIdentifierHelper()
						.toIdentifier( implicitName )
		);
	}

	private static String implicitSequenceName(
			String rootTableName,
			Map<?, ?> configValues,
			ServiceRegistry serviceRegistry) {
		final String explicitSuffix = getString( CONFIG_SEQUENCE_PER_ENTITY_SUFFIX, configValues );
		final String base = getString( IMPLICIT_NAME_BASE, configValues, rootTableName );

		if ( isNotEmpty( explicitSuffix ) ) {
			// an "implicit name suffix" was specified
			if ( isNotEmpty( base ) ) {
				return isQuoted( base )
						? "`" + unQuote( base ) + explicitSuffix + "`"
						: base + explicitSuffix;
			}
		}

		final String annotationGeneratorName = getString( GENERATOR_NAME, configValues );
		if ( isNotEmpty( annotationGeneratorName ) ) {
			return annotationGeneratorName;
		}
		else if ( isNotEmpty( base ) ) {
			return isQuoted( base )
					? "`" + unQuote( base ) + DEF_SEQUENCE_SUFFIX + "`"
					: base + DEF_SEQUENCE_SUFFIX;
		}
		else {
			throw new MappingException( "Unable to determine implicit sequence name; target table - " + rootTableName );
		}
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
			return new QualifiedNameParser.NameParts(
					catalogName,
					schemaName,
					serviceRegistry.requireService( JdbcEnvironment.class )
							.getIdentifierHelper()
							.toIdentifier( tableName )
			);
		}
	}

	private static String implicitTableName(Map<?, ?> configValues) {
		final String generatorName = getString( GENERATOR_NAME, configValues );
		return isNotEmpty( generatorName ) ? generatorName : DEF_TABLE;
	}

}
