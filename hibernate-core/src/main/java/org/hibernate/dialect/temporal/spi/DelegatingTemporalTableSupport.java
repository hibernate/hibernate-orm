/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.spi;

import java.util.List;

import org.hibernate.SPI;
import org.hibernate.temporal.TemporalTableStrategy;
import jakarta.annotation.Nullable;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base which forwards the complete temporal-table contract to one
/// immutable delegate.
///
/// Extend this class when a stock profile matches except for a small number of
/// operations. Override only those differences and let every other operation
/// continue to delegate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingTemporalTableSupport implements TemporalTableSupport {
	private final TemporalTableSupport delegate;

	/// Create a selectively overriding strategy around a non-null delegate.
	@SPI(IMPLEMENT)
	protected DelegatingTemporalTableSupport(TemporalTableSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override
	public boolean supportsNativeTemporalTables() {
		return delegate.supportsNativeTemporalTables();
	}

	@Override
	public int getTemporalColumnType() {
		return delegate.getTemporalColumnType();
	}

	@Override
	public int getTemporalColumnPrecision() {
		return delegate.getTemporalColumnPrecision();
	}

	@Override
	public @Nullable String getTemporalTableOptions(TemporalTableDdlRequest request) {
		return delegate.getTemporalTableOptions( request );
	}

	@Override
	public boolean suppressesTemporalTablePrimaryKeys(boolean partitioned) {
		return delegate.suppressesTemporalTablePrimaryKeys( partitioned );
	}

	@Override
	public boolean supportsTemporalTablePartitioning() {
		return delegate.supportsTemporalTablePartitioning();
	}

	@Override
	public List<TemporalTableAuxiliaryObject> getTemporalTableAuxiliaryObjects(TemporalTableDdlRequest request) {
		return delegate.getTemporalTableAuxiliaryObjects( request );
	}

	@Override
	public @Nullable String getExtraTemporalTableDeclarations(TemporalTableDdlRequest request) {
		return delegate.getExtraTemporalTableDeclarations( request );
	}

	@Override
	public boolean createTemporalTableCheckConstraint(TemporalTableStrategy strategy) {
		return delegate.createTemporalTableCheckConstraint( strategy );
	}

	@Override
	public String getAsOfOperator(TemporalTableStrategy strategy) {
		return delegate.getAsOfOperator( strategy );
	}

	@Override
	public boolean useAsOfOperator(TemporalTableStrategy strategy) {
		return delegate.useAsOfOperator( strategy );
	}

	@Override
	public boolean useAsOfOperatorForCurrent(TemporalTableStrategy strategy) {
		return delegate.useAsOfOperatorForCurrent( strategy );
	}

	@Override
	public boolean useTemporalRestriction(TemporalRestrictionRequest request) {
		return delegate.useTemporalRestriction( request );
	}

	@Override
	public @Nullable String getTemporalExclusionColumnOption() {
		return delegate.getTemporalExclusionColumnOption();
	}

	@Override
	public TemporalTableStrategy getDefaultTemporalTableStrategy() {
		return delegate.getDefaultTemporalTableStrategy();
	}
}
