/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.util.List;

import org.hibernate.dialect.temporal.spi.DelegatingTemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.dialect.temporal.spi.TemporalTableSupports;
import org.hibernate.type.SqlTypes;

import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.TABLE;

/// Provider-owned temporal-table extension used by the standalone Dialect
/// fixture.
///
/// The fixture composes the standard profile and adds one table-scoped
/// auxiliary object without depending on a Hibernate vendor implementation.
///
/// @author Steve Ebersole
public final class ExampleTemporalTableSupport extends DelegatingTemporalTableSupport {
	static final ExampleTemporalTableSupport INSTANCE = new ExampleTemporalTableSupport();

	private ExampleTemporalTableSupport() {
		super( TemporalTableSupports.standard( SqlTypes.TIMESTAMP, 6, true ) );
	}

	@Override
	public List<TemporalTableAuxiliaryObject> getTemporalTableAuxiliaryObjects(
			TemporalTableDdlRequest request) {
		return List.of(
				new TemporalTableAuxiliaryObject(
						"fixture-temporal-audit",
						TABLE,
						false,
						List.of( "create fixture temporal audit for " + request.tableName() ),
						List.of( "drop fixture temporal audit for " + request.tableName() )
				)
		);
	}
}
