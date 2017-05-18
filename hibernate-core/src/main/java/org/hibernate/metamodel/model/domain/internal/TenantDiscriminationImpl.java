/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;

/**
 * @author Steve Ebersole
 */
public class TenantDiscriminationImpl implements TenantDiscrimination {
	private final Column column;
	private final boolean isShared;
	private final boolean useParameterBinding;

	public TenantDiscriminationImpl(Column column, boolean isShared, boolean useParameterBinding) {
		this.column = column;
		this.isShared = isShared;
		this.useParameterBinding = useParameterBinding;
	}

	@Override
	public Column getColumn() {
		return column;
	}

	@Override
	public boolean isShared() {
		return isShared;
	}

	@Override
	public boolean isUseParameterBinding() {
		return useParameterBinding;
	}
}
