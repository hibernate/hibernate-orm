/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.collectionelement.ordered;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ElementCollectionSortingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Address.class };
	}

	@Test
	public void testSortingElementCollectionSyntax() {
		Session session = openSession();
		session.beginTransaction();

		session.createQuery( "from Person p join fetch p.nickNamesAscendingNaturalSort" ).list();
		session.createQuery( "from Person p join fetch p.nickNamesDescendingNaturalSort" ).list();

		session.createQuery( "from Person p join fetch p.addressesAscendingNaturalSort" ).list();
		session.createQuery( "from Person p join fetch p.addressesDescendingNaturalSort" ).list();
		session.createQuery( "from Person p join fetch p.addressesCityAscendingSort" ).list();
		session.createQuery( "from Person p join fetch p.addressesCityDescendingSort" ).list();

		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-6875" )
	public void testSortingEmbeddableCollectionOfPrimitives() {
		final Session session = openSession();
		session.beginTransaction();

		final Person steve = new Person();
		steve.setName( "Steve" );
		steve.getNickNamesAscendingNaturalSort().add( "sebersole" );
		steve.getNickNamesAscendingNaturalSort().add( "ebersole" );
		steve.getNickNamesDescendingNaturalSort().add( "ebersole" );
		steve.getNickNamesDescendingNaturalSort().add( "sebersole" );

		final Person lukasz = new Person();
		lukasz.setName( "Lukasz" );
		lukasz.getNickNamesAscendingNaturalSort().add( "antoniak" );
		lukasz.getNickNamesAscendingNaturalSort().add( "lantoniak" );
		lukasz.getNickNamesDescendingNaturalSort().add( "lantoniak" );
		lukasz.getNickNamesDescendingNaturalSort().add( "antoniak" );

		session.save( steve );
		session.save( lukasz );
		session.flush();

		session.clear();

		final List<String> lukaszNamesAsc = Arrays.asList( "antoniak", "lantoniak" );
		final List<String> lukaszNamesDesc = Arrays.asList( "lantoniak", "antoniak" );
		final List<String> steveNamesAsc = Arrays.asList( "ebersole", "sebersole" );
		final List<String> steveNamesDesc = Arrays.asList( "sebersole", "ebersole" );

		// Testing object graph navigation. Lazy loading collections.
		checkPersonNickNames( lukaszNamesAsc, lukaszNamesDesc, (Person) session.get( Person.class, lukasz.getId() ) );
		checkPersonNickNames( steveNamesAsc, steveNamesDesc, (Person) session.get( Person.class, steve.getId() ) );

		session.clear();

		// Testing HQL query. Eagerly fetching nicknames.
		final List<Person> result = session.createQuery(
				"select distinct p from Person p join fetch p.nickNamesAscendingNaturalSort join fetch p.nickNamesDescendingNaturalSort order by p.name"
		).list();
		Assert.assertEquals( 2, result.size() );
		checkPersonNickNames( lukaszNamesAsc, lukaszNamesDesc, result.get( 0 ) );
		checkPersonNickNames( steveNamesAsc, steveNamesDesc, result.get( 1 ) );

		// Metadata verification.
		checkSQLOrderBy( session, Person.class.getName(), "nickNamesAscendingNaturalSort", "asc" );
		checkSQLOrderBy( session, Person.class.getName(), "nickNamesDescendingNaturalSort", "desc" );

		session.getTransaction().rollback();
		session.close();
	}

	private void checkSQLOrderBy(Session session, String entityName, String propertyName, String order) {
		String roleName = entityName + "." + propertyName;
		String alias = "alias1";
		BasicCollectionPersister collectionPersister = (BasicCollectionPersister) session.getSessionFactory().getCollectionMetadata( roleName );
		Assert.assertTrue( collectionPersister.hasOrdering() );
		Assert.assertEquals( alias + "." + propertyName + " " + order, collectionPersister.getSQLOrderByString( alias ) );
	}

	private void checkPersonNickNames(List<String> expectedAscending, List<String> expectedDescending, Person person) {
		// Comparing lists to verify ordering.
		Assert.assertEquals( expectedAscending, new ArrayList<String>( person.getNickNamesAscendingNaturalSort() ) );
		Assert.assertEquals( expectedDescending, new ArrayList<String>( person.getNickNamesDescendingNaturalSort() ) );
	}
}
