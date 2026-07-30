/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.model.AggregateMappingIntent;
import org.hibernate.boot.mapping.internal.model.AggregateMemberContainer;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * Binder-side target for component member materialization.
 * <p>
 * Normal component members target the owner or collection table. Aggregate
 * component members target their logical aggregate-member container; their
 * mapping values retain the containing table reference required by the legacy
 * runtime projection, but their member columns are not registered with that
 * physical table.
 *
 * @since 9.0
 * @author Steve Ebersole
 */
public record ComponentMemberTarget(
		Kind kind,
		Table table,
		AggregateMemberContainer aggregateMemberContainer) {
	public enum Kind {
		TABLE,
		AGGREGATE_MEMBER
	}

	public static ComponentMemberTarget forSource(ComponentSource source, Table table) {
		final AggregateMappingIntent intent = source.aggregateMappingIntent();
		return intent.isAggregate()
				? new ComponentMemberTarget( Kind.AGGREGATE_MEMBER, table, AggregateMemberContainer.from( intent ) )
				: new ComponentMemberTarget( Kind.TABLE, table, null );
	}

	public boolean isAggregateMemberTarget() {
		return kind == Kind.AGGREGATE_MEMBER;
	}

	public void registerMemberColumn(Column column) {
		if ( aggregateMemberContainer != null ) {
			aggregateMemberContainer.registerColumn( column );
		}
	}
}
