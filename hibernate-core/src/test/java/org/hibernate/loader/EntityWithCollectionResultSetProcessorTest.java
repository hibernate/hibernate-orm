/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.internal.EntityLoadQueryBuilderImpl;
import org.hibernate.loader.internal.ResultSetProcessorImpl;
import org.hibernate.loader.plan.internal.SingleRootReturnLoadPlanBuilderStrategy;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.LoadPlanBuilder;
import org.hibernate.loader.spi.NamedParameterContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class EntityWithCollectionResultSetProcessorTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	public void testEntityWithSet() throws Exception {
		final EntityPersister entityPersister = sessionFactory().getEntityPersister( Person.class.getName() );

		// create some test data
		Session session = openSession();
		session.beginTransaction();
		Person person = new Person();
		person.id = 1;
		person.name = "John Doe";
		person.nickNames.add( "Jack" );
		person.nickNames.add( "Johnny" );
		session.save( person );
		session.getTransaction().commit();
		session.close();

		{
			final SingleRootReturnLoadPlanBuilderStrategy strategy = new SingleRootReturnLoadPlanBuilderStrategy(
					sessionFactory(),
					LoadQueryInfluencers.NONE,
					"abc",
					0
			);
			final LoadPlan plan = LoadPlanBuilder.buildRootEntityLoadPlan( strategy, entityPersister );
			final EntityLoadQueryBuilderImpl queryBuilder = new EntityLoadQueryBuilderImpl(
					sessionFactory(),
					LoadQueryInfluencers.NONE,
					plan
			);
			final String sql = queryBuilder.generateSql( 1 );

			final ResultSetProcessorImpl resultSetProcessor = new ResultSetProcessorImpl( plan );
			final List results = new ArrayList();

			final Session workSession = openSession();
			workSession.beginTransaction();
			workSession.doWork(
					new Work() {
						@Override
						public void execute(Connection connection) throws SQLException {
							PreparedStatement ps = connection.prepareStatement( sql );
							ps.setInt( 1, 1 );
							ResultSet resultSet = ps.executeQuery();
							results.addAll(
									resultSetProcessor.extractResults(
											resultSet,
											(SessionImplementor) workSession,
											new QueryParameters(),
											new NamedParameterContext() {
												@Override
												public int[] getNamedParameterLocations(String name) {
													return new int[0];
												}
											},
											true,
											false,
											null,
											null
									)
							);
							resultSet.close();
							ps.close();
						}
					}
			);
			assertEquals( 2, results.size() );
			Object result1 = results.get( 0 );
			assertSame( result1, results.get( 1 ) );
			assertNotNull( result1 );

			Person workPerson = ExtraAssertions.assertTyping( Person.class, result1 );
			assertEquals( 1, workPerson.id.intValue() );
			assertEquals( person.name, workPerson.name );
			assertTrue( Hibernate.isInitialized( workPerson.nickNames ) );
			assertEquals( 2, workPerson.nickNames.size() );
			assertEquals( person.nickNames, workPerson.nickNames );
			workSession.getTransaction().commit();
			workSession.close();
		}

		// clean up test data
		session = openSession();
		session.beginTransaction();
		session.delete( person );
		session.getTransaction().commit();
		session.close();
	}

	@Entity( name = "Person" )
	public static class Person {
		@Id
		private Integer id;
		private String name;
		@ElementCollection( fetch = FetchType.EAGER )
		private Set<String> nickNames = new HashSet<String>();
	}
}
