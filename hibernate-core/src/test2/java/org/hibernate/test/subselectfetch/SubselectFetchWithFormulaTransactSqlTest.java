/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.subselectfetch;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.Collection;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RequiresDialect({SQLServerDialect.class,SybaseDialect.class})
public class SubselectFetchWithFormulaTransactSqlTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected String getBaseForMappings() {
		return "";
	}

	@Override
	protected String[] getMappings() {
		return new String[] {
				"org/hibernate/test/subselectfetch/NameTransactSql.hbm.xml",
				"org/hibernate/test/subselectfetch/Value.hbm.xml"
		};
	}

	@Before
	public void before() {
		Session session = openSession();
		session.getTransaction().begin();

		Name chris = new Name();
		chris.setId( 1 );
		chris.setName( "chris" );
		Value cat = new Value();
		cat.setId(1);
		cat.setName( chris );
		cat.setValue( "cat" );
		Value canary = new Value();
		canary.setId( 2 );
		canary.setName( chris );
		canary.setValue( "canary" );

		session.persist( chris );
		session.persist( cat );
		session.persist( canary );

		Name sam = new Name();
		sam.setId(2);
		sam.setName( "sam" );
		Value seal = new Value();
		seal.setId( 3 );
		seal.setName( sam );
		seal.setValue( "seal" );
		Value snake = new Value();
		snake.setId( 4 );
		snake.setName( sam );
		snake.setValue( "snake" );

		session.persist( sam );
		session.persist(seal);
		session.persist( snake );

		session.getTransaction().commit();
		session.close();
	}

	@After
	public void after() {
		Session session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete Value" ).executeUpdate();
		session.createQuery( "delete Name" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}


	@Test
	public void checkSubselectWithFormula() throws Exception {
		// as a pre-condition make sure that subselect fetching is enabled for the collection...
		Collection collectionBinding = metadata().getCollectionBinding( Name.class.getName() + ".values" );
		assertThat( collectionBinding.isSubselectLoadable(), is( true ) );

		// Now force the subselect fetch and make sure we do not get SQL errors
		Session session = openSession();
		session.getTransaction().begin();
		List results = session.createCriteria(Name.class).list();
		for (Object result : results) {
			Name name = (Name) result;
			name.getValues().size();
		}
		session.getTransaction().commit();
		session.close();
	}

}
