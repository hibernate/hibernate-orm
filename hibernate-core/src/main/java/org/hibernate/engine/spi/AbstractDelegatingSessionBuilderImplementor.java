/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import org.hibernate.SessionBuilder;

/**
 * Base class for {@link SessionBuilderImplementor} implementations that wish to implement only parts of that contract
 * themselves while forwarding other method invocations to a delegate instance.
 *
 * @author Gunnar Morling
 */
@SuppressWarnings("unused")
public abstract class AbstractDelegatingSessionBuilderImplementor
		extends AbstractDelegatingSessionBuilder
		implements SessionBuilderImplementor {

	private final SessionBuilderImplementor delegate;

	public AbstractDelegatingSessionBuilderImplementor(SessionBuilderImplementor delegate) {
		super( delegate );
		this.delegate = delegate;
	}

	@Override
	public SessionBuilder owner(SessionOwner sessionOwner) {
		delegate.owner( sessionOwner );
		return this;
	}
}
