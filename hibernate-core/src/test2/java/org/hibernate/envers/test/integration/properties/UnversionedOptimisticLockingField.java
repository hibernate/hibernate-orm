/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.properties;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

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
		Iterator pi = pc.getPropertyIterator();
		while ( pi.hasNext() ) {
			Property p = (Property) pi.next();
			assert !"optLocking".equals( p.getName() );
		}
	}
}
