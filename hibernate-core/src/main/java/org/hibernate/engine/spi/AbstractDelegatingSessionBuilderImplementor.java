/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import org.hibernate.SessionBuilder;

/**
 * Base class for {@link SessionBuilder} implementations that wish to implement only parts of that contract
 * themselves while forwarding other method invocations to a delegate instance.
 *
 * @author Gunnar Morling
 */
public abstract class AbstractDelegatingSessionBuilderImplementor<T extends SessionBuilder<T>>
		extends AbstractDelegatingSessionBuilder<T>
		implements SessionBuilder<T> {

	public AbstractDelegatingSessionBuilderImplementor(SessionBuilder<T> delegate) {
		super( delegate );
	}
}
