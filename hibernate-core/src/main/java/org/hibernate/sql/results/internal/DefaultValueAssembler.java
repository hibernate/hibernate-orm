package org.hibernate.sql.results.internal;

import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Supplies the default value for a primitive entity attribute omitted from the SQL result.
 *
 * @author Gavin King
 */
public class DefaultValueAssembler<J> implements DomainResultAssembler<J> {
	private final JavaType<J> javaType;
	private final J defaultValue;

	public DefaultValueAssembler(JavaType<J> javaType) {
		this.javaType = javaType;
		this.defaultValue = javaType.getDefaultValue();
	}

	@Override
	public J assemble(RowProcessingState rowProcessingState) {
		return defaultValue;
	}

	@Override
	public JavaType<J> getAssembledJavaType() {
		return javaType;
	}
}
