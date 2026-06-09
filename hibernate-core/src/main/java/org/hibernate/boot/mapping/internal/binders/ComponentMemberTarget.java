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
 * Normal component members target the owner or collection table.  Aggregate
 * component members have a logical aggregate-member target, but this first
 * bridge still delegates to the same table path while the legacy
 * {@code Component}/{@code AggregateColumn} projection remains authoritative.
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
