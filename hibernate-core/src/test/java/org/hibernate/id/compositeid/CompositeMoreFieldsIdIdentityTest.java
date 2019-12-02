/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.compositeid;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Fabio Massimo Ercoli
 */
@RequiresDialectFeature(DialectChecks.SupportsCompositeNestedIdentityColumns.class)
@TestForIssue(jiraKey = "HHH-9662")
public class CompositeMoreFieldsIdIdentityTest extends BaseCoreFunctionalTestCase {

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, session -> {
			CompositeMoreFieldsIdIdentityEntity entity = new CompositeMoreFieldsIdIdentityEntity();
			entity.setId( new CompositeMoreFieldsId( 739L, 1731730L ) );

			session.persist( entity );

			assertNotNull( entity.getId().getId() );
			assertNotNull( entity.getId().getAnotherId() );
			assertNotNull( entity.getId().getGeneratedId() );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { CompositeMoreFieldsIdIdentityEntity.class };
	}
}
