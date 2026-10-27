package org.hibernate.sql.exec.internal;

import org.hibernate.metamodel.mapping.JdbcMapping;

/**
 * @author Steve Ebersole
 */
public class JdbcParameterImpl<J> extends AbstractJdbcParameter<J> {

	public JdbcParameterImpl(JdbcMapping<J> jdbcMapping) {
		super( jdbcMapping );
	}

	public JdbcParameterImpl(JdbcMapping<J> jdbcMapping, Integer parameterId) {
		super( jdbcMapping, parameterId );
	}
}
