/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.List;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
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
		@SuppressWarnings("unchecked") List<PersistentClass> pcs = collectionToList( metadata().getEntityBindings() );
		Assert.assertEquals( 1, pcs.size() );
		Assert.assertTrue( pcs.get( 0 ).getClassName().contains( "BasicTestEntity3" ) );
	}
}
