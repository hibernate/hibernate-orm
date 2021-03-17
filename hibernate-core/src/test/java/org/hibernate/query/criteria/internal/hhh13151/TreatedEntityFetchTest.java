/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.hhh13151;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class TreatedEntityFetchTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				SubEntity.class,
				SuperEntity.class,
				SideEntity.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
		// configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Before
	public void prepareEntities() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( new SubEntity().setSubField( new SideEntity( "testName" ) ) );
		tx.commit();
		s.close();
	}

	@Test
	public void hhh13151Test() throws Exception {
		Session s = openSession();

		// Prepare Query
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<SuperEntity> criteria = cb.createQuery( SuperEntity.class );
		Root<SuperEntity> root = criteria.from( SuperEntity.class );
		cb.treat( root, SubEntity.class ).fetch( "subField" );

		// Execute
		Transaction tx = s.beginTransaction();
		List<SuperEntity> result = s.createQuery( criteria ).getResultList();
		tx.commit();
		s.close();

		// Check results
		SideEntity subField = ( (SubEntity) result.get( 0 ) ).getSubField();
		String name = subField.getName();
		assertTrue( name != null );
	}
}
