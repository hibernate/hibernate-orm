/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

/**
 * @author Christian Beikov
 */
public class PostgreSQLJsonbJdbcType extends AbstractPostgreSQLJsonJdbcType {

	public static final PostgreSQLJsonbJdbcType INSTANCE = new PostgreSQLJsonbJdbcType( null );

	public PostgreSQLJsonbJdbcType(EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType, "jsonb" );
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(EmbeddableMappingType mappingType, String sqlType) {
		return new PostgreSQLJsonbJdbcType( mappingType );
	}
}
