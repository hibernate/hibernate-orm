/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or it's affiliates, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.test.cache.infinispan.entity;
import org.hibernate.cache.spi.access.AccessType;

import static org.junit.Assert.fail;

/**
 * Tests for the "extra API" in EntityRegionAccessStrategy;
 * <p/>
 * By "extra API" we mean those methods that are superfluous to the
 * function of the Infinispan integration, where the impl is a no-op or a static
 * false return value, UnsupportedOperationException, etc.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class ReadOnlyExtraAPITestCase extends TransactionalExtraAPITestCase {
	@Override
	protected AccessType getAccessType() {
		return AccessType.READ_ONLY;
	}

	@Override
	public void testAfterUpdate() {
		try {
			getEntityAccessStrategy().afterUpdate(
               KEY, VALUE2, 1, 2, new MockSoftLock());
			fail( "Call to afterUpdate did not throw exception" );
		}
		catch (UnsupportedOperationException expected) {
		}
	}

}
