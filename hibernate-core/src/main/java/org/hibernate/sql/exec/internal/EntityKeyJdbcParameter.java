package org.hibernate.sql.exec.internal;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.SelectableMapping;

/// A JDBC parameter that extracts an identifier or association key from an entity argument.
public final class EntityKeyJdbcParameter extends SqlTypedMappingJdbcParameter {
	private final MappingModelExpressible<?> entityMapping;

	public EntityKeyJdbcParameter(SelectableMapping selectable, int parameterId, MappingModelExpressible<?> entityMapping) {
		super( selectable, parameterId );
		this.entityMapping = entityMapping;
	}

	@Override
	public MappingModelExpressible<?> getEntityMapping() {
		return entityMapping;
	}
}
