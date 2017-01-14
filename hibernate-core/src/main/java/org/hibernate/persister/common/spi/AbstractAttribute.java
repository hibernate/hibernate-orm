/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.lang.reflect.Member;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.spi.Type;

/**
 * Base class for Attribute implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttribute<O,T> implements Attribute<O,T> {
	private final ManagedTypeImplementor<O> container;
	private final String name;
	private final PropertyAccess access;

	public AbstractAttribute(
			ManagedTypeImplementor<O> container,
			String name,
			PropertyAccess access) {
		this.container = container;
		this.name = name;
		this.access = access;
	}

	@Override
	public Type getExportedDomainType() {
		return null;
	}

	@Override
	public ManagedTypeImplementor<O> getSource() {
		return container;
	}

	@Override
	public String getAttributeName() {
		return name;
	}

	@Override
	public String getNavigableName() {
		return getAttributeName();
	}

	@Override
	public Member getJavaMember() {
		return access.getGetter().getMember();
	}
}
