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
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.id.enhanced.TableGenerator.DEF_TABLE;

/**
 * ImplicitDatabaseObjectNamingStrategy using a single structure for
 * all implicit names:<ul>
 *     <li>{@value ImplicitDatabaseObjectNamingStrategy#DEF_SEQUENCE} for sequences</li>
 *     <li>{@value TableGenerator#DEF_TABLE} for tables</li>
 * </ul>
 *
 * @author Andrea Boriero
 */
public class SingleNamingStrategy implements ImplicitDatabaseObjectNamingStrategy {
	public static final String STRATEGY_NAME = "single";

	@Override
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
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		return new QualifiedNameParser.NameParts(
				catalogName,
				schemaName,
				jdbcEnvironment.getIdentifierHelper().toIdentifier( DEF_TABLE )
		);
	}
}
