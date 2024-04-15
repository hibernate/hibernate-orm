/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contract for something that can build a SessionFactory.
 *
 * Used by SessionFactoryScopeExtension to create the
 * SessionFactoryScope.
 *
 * Generally speaking, a test class would implement SessionFactoryScopeContainer
 * and return the SessionFactoryProducer to be used for those tests.
 * The SessionFactoryProducer is then used to build the SessionFactoryScope
 * which is injected back into the SessionFactoryScopeContainer
 *
 * @see SessionFactoryExtension
 * @see SessionFactoryScope
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryProducer {
	SessionFactoryImplementor produceSessionFactory(MetadataImplementor model);
}
