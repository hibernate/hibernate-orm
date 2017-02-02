/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.basic;

import java.util.List;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.mapping.PersistentClass;

import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.envers.internal.tools.Tools.collectionToList;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NoneAudited extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity3.class};
	}

	@Test
	public void testRevisionInfoTableNotCreated() {
		@SuppressWarnings({"unchecked"}) List<PersistentClass> pcs = collectionToList( metadata().getEntityBindings() );
		Assert.assertEquals( 1, pcs.size() );
		Assert.assertTrue( pcs.get( 0 ).getClassName().contains( "BasicTestEntity3" ) );
	}
}