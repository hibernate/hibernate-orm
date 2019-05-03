/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property;

import org.hibernate.metamodel.model.domain.internal.NoopMember;
import org.hibernate.property.access.internal.PropertyAccessStrategyNoopImpl;
import org.hibernate.property.access.spi.PropertyAccess;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Hill
 */
public class NoopPropertyAccessorTest extends BaseUnitTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-13389")
	public void testAccessorProperties() {
		PropertyAccessStrategyNoopImpl accessStrategy = PropertyAccessStrategyNoopImpl.INSTANCE;
		final PropertyAccess access = accessStrategy.buildPropertyAccess( Object.class, "foo" );
		assertNull( access.getGetter().getMethod() );
		assertNull( access.getGetter().getMethodName() );
		assertSame( Object.class, access.getGetter().getReturnType() );
		assertTrue( access.getGetter().getMember() instanceof NoopMember );
		assertNull( access.getSetter().getMethod() );
		assertNull( access.getSetter().getMethodName() );
	}
}
