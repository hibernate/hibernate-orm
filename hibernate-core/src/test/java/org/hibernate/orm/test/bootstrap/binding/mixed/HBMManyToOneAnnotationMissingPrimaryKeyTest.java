/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.mixed;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * https://hibernate.atlassian.net/browse/HHH-11502
 *
 * @author Russ Tennant (russ@venturetech.net)
 */
public class HBMManyToOneAnnotationMissingPrimaryKeyTest extends BaseNonConfigCoreFunctionalTestCase
{
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				AnnotationEntity.class
		};
	}

	@Override
	protected String[] getMappings() {
		return new String[]{
				"HBMEntity.hbm.xml"
		};
	}

	@Override
	protected String getBaseForMappings() {
		return "/org/hibernate/orm/test/bootstrap/binding/mixed/";
	}

	/**
	 * Test to trigger the NullPointerException in the ModelBinder.
	 * @throws Exception on error.
	 */
	@Test
	public void hhh11502() throws Exception {
		Assert.assertTrue(true);
	}
}
