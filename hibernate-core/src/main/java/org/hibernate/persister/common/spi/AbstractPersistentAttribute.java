/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.lang.reflect.Member;

import org.hibernate.persister.common.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.Type;

/**
 * Base class for Attribute implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPersistentAttribute<O,T> implements PersistentAttribute<O,T> {
	private final ManagedTypeImplementor<O> container;
	private final Type<T> ormType;
	private final PropertyAccess access;

	private final NavigableRole navigableRole;

	public AbstractPersistentAttribute(
			ManagedTypeImplementor<O> container,
			String name,
			Type<T> ormType,
			PropertyAccess access) {
		this.container = container;
		this.ormType = ormType;
		this.access = access;

		this.navigableRole = container.getNavigableRole().append( name );
	}

	@Override
	public ManagedTypeImplementor<O> getContainer() {
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
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getOrmType().getJavaTypeDescriptor();
	}

	public Type<T> getOrmType() {
		return ormType;
	}

	@Override
	public Member getJavaMember() {
		return access.getGetter().getMember();
	}
}
