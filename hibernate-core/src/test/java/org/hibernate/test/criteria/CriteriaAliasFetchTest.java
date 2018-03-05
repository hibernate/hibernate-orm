/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Thomas Reinhardt
 */
public class CriteriaAliasFetchTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( Cat.class );
		sources.addAnnotatedClass( Kitten.class );
	}

	@Override
	protected void cleanupTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "delete from " + Kitten.class.getName() ).executeUpdate();
			session.createQuery( "delete from " + Cat.class.getName() ).executeUpdate();
		} );
	}

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {

			// make 5 cats with 3 kittens each
			for ( int i = 0; i < 5; i++ ) {
				Cat cat = new Cat();
				cat.catId = i;
				cat.name = "cat_" + i;
				session.save( cat );
				for ( int j = 0; j < 3; j++ ) {
					Kitten k = new Kitten();
					k.kittenId = 5 * i + j;
					k.name = "kitty_" + i + "_" + j;
					k.cat = cat;
					cat.kittens.add( k );
					session.save( k );
				}
			}

			session.flush();
			session.clear();
		} );
	}

	public void assertOnlyOneSelect(Criteria criteria) {
		sessionFactory().getStatistics().setStatisticsEnabled( true );
		sessionFactory().getStatistics().clear();

		try {
			List<Cat> cats = criteria.list();

			assertEquals( 5, cats.size() );

			for ( Cat cat : cats ) {
				assertEquals( 3, cat.kittens.size() );

				for ( Kitten kitten : cat.kittens ) {
					assertNotNull( kitten.cat );
				}
			}

			assertEquals( "too many statements generated", 1, sessionFactory().getStatistics().getPrepareStatementCount() );
		}
		finally {
			sessionFactory().getStatistics().setStatisticsEnabled( false );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7842")
	public void testFetchWithAlias() {
		doInHibernate( this::sessionFactory, session -> {

			assertOnlyOneSelect( session.createCriteria( Cat.class, "c" )
					.setFetchMode( "c.kittens", FetchMode.JOIN )
					.setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ) );

		} );
	}

	@Test
	public void testFixForHHH7842DoesNotBreakOldBehavior() {
		doInHibernate( this::sessionFactory, session -> {

			assertOnlyOneSelect( session.createCriteria( Cat.class )
					.setFetchMode( "kittens", FetchMode.JOIN )
					.setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ) );

		} );
	}

	@Entity(name = "Cat")
	public static class Cat {

		@Id
		public Integer catId;
		public String name;

		@OneToMany(mappedBy = "cat")
		public java.util.Set<Kitten> kittens = new java.util.HashSet<>();
	}

	@Entity(name = "Kitten")
	public static class Kitten {

		@Id
		public Integer kittenId;
		public String name;

		@ManyToOne(fetch = FetchType.LAZY)
		public Cat cat;
	}

}
