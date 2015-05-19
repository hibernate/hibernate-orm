/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.collection;

import org.hibernate.cache.spi.access.AccessType;

/**
 * Base class for tests of TRANSACTIONAL access.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public abstract class AbstractReadOnlyAccessTestCase extends AbstractCollectionRegionAccessStrategyTestCase {
    @Override
    protected AccessType getAccessType() {
        return AccessType.READ_ONLY;
    }
}
