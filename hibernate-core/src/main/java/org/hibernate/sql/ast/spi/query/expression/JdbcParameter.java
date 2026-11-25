package org.hibernate.sql.ast.spi.query.expression;

import jakarta.annotation.Nullable;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * @author Steve Ebersole
 */
public interface JdbcParameter extends Expression {
	JdbcParameterBinder getParameterBinder();
	@Nullable Integer getParameterId();

	/// The mapping used to extract the entity key at this occurrence of a domain parameter.
	default @Nullable MappingModelExpressible<?> getEntityMapping() {
		return null;
	}
}
