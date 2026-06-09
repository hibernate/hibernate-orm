/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.mapping.Column;

import static java.util.Collections.unmodifiableList;

/**
 * Internal target for aggregate component members.
 * <p>
 * The current pipeline still projects aggregate members into the legacy
 * {@code Component}/{@code AggregateColumn} bridge, but keeping the logical
 * member container explicit gives binder/materialization code a stable handoff
 * to move away from temporary owner-table columns.
 *
 * @since 9.0
 * @author Steve Ebersole
 */
public record AggregateMemberContainer(
		ComponentSource.Kind sourceKind,
		AggregateMappingIntent.AggregateKind aggregateKind,
		String componentTypeName,
		String sourceAttributeName,
		List<Member> members,
		List<Column> memberColumns) {
	public AggregateMemberContainer {
		members = List.copyOf( members );
		memberColumns = new ArrayList<>( memberColumns );
	}

	public static AggregateMemberContainer from(AggregateMappingIntent intent) {
		final ComponentSource source = intent.source();
		return new AggregateMemberContainer(
				source.kind(),
				intent.aggregateKind(),
				source.componentType().getName(),
				source.sourceMember() == null ? null : source.sourceMember().resolveAttributeName(),
				source.members().stream()
						.map( member -> new Member(
								member.fullPath(),
								member.member().resolveAttributeName(),
								member.type().determineRawClass().getName()
						) )
						.toList(),
				new ArrayList<>()
		);
	}

	public void registerColumn(Column column) {
		memberColumns.add( column );
	}

	public List<Column> memberColumns() {
		return unmodifiableList( memberColumns );
	}

	public record Member(String path, String attributeName, String typeName) {
	}
}
