/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

/**
 * Convenience base class for custom implementors of {@link SessionFactoryBuilderImplementor}, using delegation
 *
 * @author Guillaume Smet
 * @param <T> The type of a specific sub-class; Allows sub-classes to narrow down the return-type of the contract methods
 * to a specialization of {@link SessionFactoryBuilderImplementor}
 */
public abstract class AbstractDelegatingSessionFactoryBuilderImplementor<T extends SessionFactoryBuilderImplementor>
		extends AbstractDelegatingSessionFactoryBuilder<T> implements SessionFactoryBuilderImplementor {

	public AbstractDelegatingSessionFactoryBuilderImplementor(SessionFactoryBuilderImplementor delegate) {
		super( delegate );
	}

	@Override
	protected SessionFactoryBuilderImplementor getDelegate() {
		return (SessionFactoryBuilderImplementor) super.getDelegate();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void markAsJpaBootstrap() {
		getDelegate().markAsJpaBootstrap();
	}

	@Override
	public void disableJtaTransactionAccess() {
		getDelegate().disableJtaTransactionAccess();
	}

	@Override
	public SessionFactoryOptions buildSessionFactoryOptions() {
		return getDelegate().buildSessionFactoryOptions();
	}
}
