/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ordered;

import java.util.Iterator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/ordered/Search.hbm.xml"
)
@SessionFactory
public class OrderByTest {

	@AfterEach
	public void tearDonw(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrderBy(SessionFactoryScope scope) {

		scope.inTransaction(
				sess -> {
					Search s = new Search( "Hibernate" );
					s.getSearchResults().add( "jboss.com" );
					s.getSearchResults().add( "hibernate.org" );
					s.getSearchResults().add( "HiA" );
					sess.persist( s );
					sess.flush();

					sess.clear();
					CriteriaBuilder criteriaBuilder = sess.getCriteriaBuilder();
					CriteriaQuery<Search> criteria = criteriaBuilder.createQuery( Search.class );
					criteria.from( Search.class );
					s = sess.createQuery( criteria ).uniqueResult();
//					s = (Search) sess.createCriteria( Search.class ).uniqueResult();
					assertFalse( Hibernate.isInitialized( s.getSearchResults() ) );
					Iterator iter = s.getSearchResults().iterator();
					assertThat( iter.next(), is( "HiA" ) );
					assertThat( iter.next(), is( "hibernate.org" ) );
					assertThat( iter.next(), is( "jboss.com" ) );
					assertFalse( iter.hasNext() );

					sess.clear();
					criteria = criteriaBuilder.createQuery( Search.class );
					criteria.from( Search.class ).fetch( "searchResults", JoinType.LEFT );
					s = sess.createQuery( criteria ).uniqueResult();
//					s = (Search) sess.createCriteria( Search.class )
//							.setFetchMode( "searchResults", FetchMode.JOIN )
//							.uniqueResult();
					assertTrue( Hibernate.isInitialized( s.getSearchResults() ) );
					iter = s.getSearchResults().iterator();
					assertThat( iter.next(), is( "HiA" ) );
					assertThat( iter.next(), is( "hibernate.org" ) );
					assertThat( iter.next(), is( "jboss.com" ) );
					assertFalse( iter.hasNext() );

					sess.clear();
					s = (Search) sess.createQuery( "from Search s left join fetch s.searchResults" )
							.uniqueResult();
					assertTrue( Hibernate.isInitialized( s.getSearchResults() ) );
					iter = s.getSearchResults().iterator();
					assertThat( iter.next(), is( "HiA" ) );
					assertThat( iter.next(), is( "hibernate.org" ) );
					assertThat( iter.next(), is( "jboss.com" ) );
					assertFalse( iter.hasNext() );

		/*sess.clear();
		s = (Search) sess.createCriteria(Search.class).uniqueResult();
		assertFalse( Hibernate.isInitialized( s.getSearchResults() ) );
		iter = sess.createFilter( s.getSearchResults(), "").iterate();
		assertEquals( iter.next(), "HiA" );
		assertEquals( iter.next(), "hibernate.org" );
		assertEquals( iter.next(), "jboss.com" );
		assertFalse( iter.hasNext() );*/

					sess.remove( s );
				}
		);
	}

}
