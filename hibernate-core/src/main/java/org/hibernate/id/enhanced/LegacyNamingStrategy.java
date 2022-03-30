/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.util.Map;

import jakarta.persistence.GeneratedValue;

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
 * Naming strategy which implements the behavior of older versions of
 * Hibernate, for the most part.
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
 *         Fall back is to use {@value DEF_SEQUENCE}
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
 */
public class LegacyNamingStrategy implements ImplicitDatabaseObjectNamingStrategy {
	public static final String STRATEGY_NAME = "legacy";

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

		if ( StringHelper.isNotEmpty( explicitSuffix ) ) {
			// an "implicit name suffix" was specified
			final String rootTableName = ConfigurationHelper.getString( PersistentIdentifierGenerator.TABLE, configValues );
			final String base = ConfigurationHelper.getString( IMPLICIT_NAME_BASE, configValues, rootTableName );
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
		final String implicitName = implicitTableName( configValues );

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

	private String implicitTableName(Map<?, ?> configValues) {
		final String annotationName = ConfigurationHelper.getString( IdentifierGenerator.GENERATOR_NAME, configValues );
		if ( StringHelper.isNotEmpty( annotationName ) ) {
			return annotationName;
		}

		return DEF_TABLE;
	}
}
