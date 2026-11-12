package org.hibernate.sql.exec.internal;

import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.usertype.internal.ContextualTemporalCompositeUserType;

/// One component of a whole COLUMN assignment. The binding builder prepares the
/// complete tuple once, before decomposing it; individual binders must not prepare
/// its primary again. Predicate parameters never use this marker.
///
/// @author Steve Ebersole
public final class TemporalColumnJdbcParameter extends SqlTypedMappingJdbcParameter {
	private final ContextualTemporalCompositeUserType<?> temporal;

	public TemporalColumnJdbcParameter(SqlTypedMapping mapping, Integer id, ContextualTemporalCompositeUserType<?> temporal) {
		super( mapping, id, false );
		this.temporal = temporal;
	}

	public Object[] prepareTuple(Object value) {
		return temporal.prepareTuple( value );
	}
}
