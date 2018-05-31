/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

/**
 * Return a standalone JTA transaction manager for WildFly transaction client
 * Known to work for WildFly 13+
 *
 * @author Scott Marlow
 *
 * @deprecated (since 5.3.1), use {@link org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform} instead
 */
@Deprecated
public class WildFlyStandAloneJtaPlatform extends JBossStandAloneJtaPlatform {

}
