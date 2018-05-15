/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class TenantDiscriminationImpl implements TenantDiscrimination {
	private static final String NAVIGABLE_NAME = "{tenantId}";

	private final IdentifiableTypeDescriptor container;
	private final BasicJavaDescriptor<String> javaDescriptor;
	private final Column column;
	private final boolean isShared;
	private final boolean useParameterBinding;

	private final NavigableRole navigableRole;

	public TenantDiscriminationImpl(
			IdentifiableTypeDescriptor container,
			BasicJavaDescriptor<String> javaDescriptor,
			Column column,
			boolean isShared,
			boolean useParameterBinding) {
		this.container = container;
		this.javaDescriptor = javaDescriptor;
		this.column = column;
		this.isShared = isShared;
		this.useParameterBinding = useParameterBinding;

		this.navigableRole = container.getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public NavigableContainer getContainer() {
		return container;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String asLoggableText() {
		return getNavigableRole().getFullPath();
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicType<String> getBasicType() {
		throw new NotYetImplementedFor6Exception();
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
