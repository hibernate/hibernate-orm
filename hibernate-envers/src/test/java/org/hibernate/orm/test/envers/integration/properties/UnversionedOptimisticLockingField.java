/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.properties;

import java.util.Arrays;
import java.util.Map;
import jakarta.persistence.EntityManager;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Nicolas Doroskevich
 */
public class UnversionedOptimisticLockingField extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {UnversionedOptimisticLockingFieldEntity.class};
	}

	@Override
	public void addConfigOptions(Map configuration) {
		super.addConfigOptions( configuration );
		configuration.put( EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD, "true" );
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		UnversionedOptimisticLockingFieldEntity olfe = new UnversionedOptimisticLockingFieldEntity( "x" );
		em.persist( olfe );
		id1 = olfe.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		olfe = em.find( UnversionedOptimisticLockingFieldEntity.class, id1 );
		olfe.setStr( "y" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionCounts() {
		assert Arrays.asList( 1, 2 ).equals(
				getAuditReader().getRevisions(
						UnversionedOptimisticLockingFieldEntity.class,
						id1
				)
		);
	}

	@Test
	public void testHistoryOfId1() {
		UnversionedOptimisticLockingFieldEntity ver1 = new UnversionedOptimisticLockingFieldEntity( id1, "x" );
		UnversionedOptimisticLockingFieldEntity ver2 = new UnversionedOptimisticLockingFieldEntity( id1, "y" );

		assert getAuditReader().find( UnversionedOptimisticLockingFieldEntity.class, id1, 1 )
				.equals( ver1 );
		assert getAuditReader().find( UnversionedOptimisticLockingFieldEntity.class, id1, 2 )
				.equals( ver2 );
	}

	@Test
	public void testMapping() {
		PersistentClass pc = metadata().getEntityBinding( UnversionedOptimisticLockingFieldEntity.class.getName() + "_AUD" );
		for ( Property p : pc.getProperties() ) {
			Assert.assertNotEquals( "optLocking", p.getName() );
		}
	}
}
