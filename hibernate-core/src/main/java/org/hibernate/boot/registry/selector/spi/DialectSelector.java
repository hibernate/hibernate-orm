/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.spi;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.hibernate.boot.registry.selector.internal.LazyServiceResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.Service;

/**
 * Service which acts as a registry for named strategy implementations.
 * <p/>
 * Strategies are more open ended than services, though a strategy managed here might very well also be a service.  The
 * strategy is any interface that has multiple, (possibly short) named implementations.
 * <p/>
 * StrategySelector manages resolution of particular implementation by (possibly short) name via the
 * {@link StrategySelector#selectStrategyImplementor} method, which is the main contract here.  As indicated in the docs of that
 * method the given name might be either a short registered name or the implementation FQN.  As an example, consider
 * resolving the {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder} implementation to use.  To use the
 * JDBC-based TransactionCoordinatorBuilder the passed name might be either {@code "jdbc"} or
 * {@code "org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl"} (which is the FQN).
 * <p/>
 * Strategy implementations can be managed by {@link StrategySelector#registerStrategyImplementor} and
 * {@link StrategySelector#unRegisterStrategyImplementor}.  Originally designed to help the OSGi use case, though no longer used there.
 * <p/>
 * The service also exposes a general typing API via {@link StrategySelector#resolveStrategy} and {@link StrategySelector#resolveDefaultableStrategy}
 * which accept implementation references rather than implementation names, allowing for a multitude of interpretations
 * of said "implementation reference".  See the docs for {@link StrategySelector#resolveDefaultableStrategy} for details.
 *
 * @author Christian Beikov
 */
public interface DialectSelector extends Service, LazyServiceResolver<Dialect> {
}
