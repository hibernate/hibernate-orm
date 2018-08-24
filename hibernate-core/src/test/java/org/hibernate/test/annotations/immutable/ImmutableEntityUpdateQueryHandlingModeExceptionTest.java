/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.immutable;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.query.Query;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12387" )
public class ImmutableEntityUpdateQueryHandlingModeExceptionTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Country.class, State.class, Photo.class,
		ImmutableEntity.class, MutableEntity.class};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE, "exception" );
	}

	@Test
	public void testBulkUpdate(){
		Country _country = doInHibernate( this::sessionFactory, session -> {
			Country country = new Country();
			country.setName("Germany");
			session.persist(country);

			return country;
		} );

		try {
			doInHibernate( this::sessionFactory, session -> {
				session.createQuery(
					"update Country " +
					"set name = :name" )
				.setParameter( "name", "N/A" )
				.executeUpdate();
			} );
			fail("Should throw HibernateException");
		}
		catch (HibernateException e) {
			assertEquals( "The query: [update Country set name = :name] attempts to update an immutable entity: [Country]", e.getMessage() );
		}

		doInHibernate( this::sessionFactory, session -> {
			Country country = session.find(Country.class, _country.getId());
			assertEquals( "Germany", country.getName() );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12927" )
	public void testImmutableSubselect() {
		doInHibernate(this::sessionFactory, session -> {
			String selector = "foo";
			ImmutableEntity trouble = new ImmutableEntity(selector);
			session.persist(trouble);

			MutableEntity entity = new MutableEntity(trouble, "start");
			session.persist(entity);

			// Change a muteable value via selection based on an immutable property
			String statement = "Update MutableEntity e set e.changeable = :changeable where e.trouble.id in " +
					"(select i.id from ImmutableEntity i where i.selector = :selector)";

			Query query = session.createQuery(statement);
			query.setParameter("changeable", "end");
			query.setParameter("selector", "foo");
			query.executeUpdate();

			session.refresh(entity);

			// Assert that the value was changed. If HHH-12927 has not been fixed an exception will be thrown
			// before we get here.
			assertEquals("end", entity.getChangeable());
		});
	}
}
