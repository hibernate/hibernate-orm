/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.SessionFactoryBuilder;

/**
 * Additional contract for SessionFactoryBuilder mainly intended for implementors
 * of SessionFactoryBuilderFactory.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryBuilderImplementor extends SessionFactoryBuilder {
	/**
	 * Indicates that the SessionFactory being built comes from JPA bootstrapping.
	 * Internally {@code false} is the assumed value.  We only need to call this to
	 * mark that as true.
	 *
	 * @deprecated (since 5.2) In fact added in 5.2 as part of consolidating JPA support
	 * directly into Hibernate contracts (SessionFactory, Session); intended to provide
	 * transition help in cases where we need to know the difference in JPA/native use for
	 * various reasons.
	 */
	@Deprecated
	void markAsJpaBootstrap();

	void disableJtaTransactionAccess();

	default void disableRefreshDetachedEntity() {
	}

	/**
	 * Build the SessionFactoryOptions that will ultimately be passed to SessionFactoryImpl constructor.
	 *
	 * @return The options.
	 */
	SessionFactoryOptions buildSessionFactoryOptions();
}
