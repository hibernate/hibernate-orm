/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Base class for Attribute implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPersistentAttribute<O,J> implements PersistentAttribute<O,J> {
	private final ManagedTypeDescriptor<O> container;
	private final JavaTypeDescriptor<J> javaTypeDescriptor;
	private final PropertyAccess access;

	private final NavigableRole navigableRole;

	public AbstractPersistentAttribute(
			ManagedTypeDescriptor<O> container,
			String name,
			JavaTypeDescriptor<J> javaTypeDescriptor,
			PropertyAccess access) {
		this.container = container;
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.access = access;

		this.navigableRole = container.getNavigableRole().append( name );
	}

	@Override
	public ManagedTypeDescriptor<O> getContainer() {
		return container;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getAttributeName() {
		return getNavigableRole().getNavigableName();
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return access;
	}

	@Override
	public JavaTypeDescriptor<J> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}
}
