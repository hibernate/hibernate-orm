/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.resultset.internal;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.Metamodel;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.query.sql.spi.QueryResultBuilder;
import org.hibernate.query.sql.spi.QueryResultBuilderScalar;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class ScalarResultDefinitionImpl implements ResultSetMappingDefinition.ScalarResult {
	private final String columnAlias;
	private final String typeName;

	public ScalarResultDefinitionImpl(String columnAlias, String typeName) {
		this.columnAlias = columnAlias;
		this.typeName = typeName;
	}

	@Override
	public String getColumnAlias() {
		return columnAlias;
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public QueryResultBuilder generateQueryResultBuilder(Metamodel metamodel) {
		final BasicType basicType = metamodel.getTypeConfiguration().getBasicTypeRegistry()
				.getBasicType( typeName );
		if ( basicType == null ) {
			throw noTypeException( typeName );
		}

		return new QueryResultBuilderScalar( columnAlias, basicType.getSqlExpressableType( metamodel.getTypeConfiguration() ) );
	}

	protected HibernateException noTypeException(String typeName) {
		throw new MappingException(
				String.format(
						"Unable to resolve type [%s] specified for native query scalar return",
						typeName
				)
		);
	}
}
