package org.hibernate.sql.exec.internal;

import jakarta.annotation.Nullable;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.spi.query.expression.SqlTypedExpression;

/**
 * @author Steve Ebersole
 */
public class SqlTypedMappingJdbcParameter<J> extends AbstractJdbcParameter<J> implements SqlTypedExpression {

	private final SqlTypedMapping sqlTypedMapping;

	public static SqlTypedMappingJdbcParameter<?> create(SqlTypedMapping sqlTypedMapping) {
		return create( sqlTypedMapping, null );
	}

	public static SqlTypedMappingJdbcParameter<?> create(SqlTypedMapping sqlTypedMapping, @Nullable Integer parameterId) {
		return new SqlTypedMappingJdbcParameter<>( sqlTypedMapping, parameterId, sqlTypedMapping.getJdbcMapping() );
	}

	protected SqlTypedMappingJdbcParameter(SqlTypedMapping sqlTypedMapping, @Nullable Integer parameterId, JdbcMapping<J> jdbcMapping) {
		super( jdbcMapping, parameterId );
		this.sqlTypedMapping = sqlTypedMapping;
	}

	@Override
	public SqlTypedMapping getSqlTypedMapping() {
		return sqlTypedMapping;
	}
}
