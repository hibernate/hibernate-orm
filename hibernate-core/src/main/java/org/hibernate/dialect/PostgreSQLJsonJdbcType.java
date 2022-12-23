/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

/**
 * @author Christian Beikov
 */
public class PostgreSQLJsonJdbcType extends AbstractPostgreSQLJsonJdbcType {

	public static final PostgreSQLJsonJdbcType INSTANCE = new PostgreSQLJsonJdbcType( null );

	private PostgreSQLJsonJdbcType(EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType, "json" );
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new PostgreSQLJsonJdbcType( mappingType );
	}

}
