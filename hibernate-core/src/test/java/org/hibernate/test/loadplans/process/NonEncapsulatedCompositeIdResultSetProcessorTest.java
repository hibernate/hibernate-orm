/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.onetoone.formula.Address;
import org.hibernate.test.onetoone.formula.Person;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class NonEncapsulatedCompositeIdResultSetProcessorTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "onetoone/formula/Person.hbm.xml" };
	}

	@Test
	public void testCompositeIdWithKeyManyToOne() throws Exception {
		final String personId = "John Doe";

		Person p = new Person();
		p.setName( personId );
		final Address a = new Address();
		a.setPerson( p );
		p.setAddress( a );
		a.setType( "HOME" );
		a.setStreet( "Main St" );
		a.setState( "Sweet Home Alabama" );
		a.setZip( "3181" );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( p );
		t.commit();
		s.close();

		final EntityPersister personPersister = sessionFactory().getEntityPersister( Person.class.getName() );
		final EntityPersister addressPersister = sessionFactory().getEntityPersister( Address.class.getName() );

		{
			final List results = getResults(
					addressPersister,
					new Callback() {
						@Override
						public void bind(PreparedStatement ps) throws SQLException {
							ps.setString( 1, personId );
							ps.setString( 2, "HOME" );
						}

						@Override
						public QueryParameters getQueryParameters() {
							QueryParameters qp = new QueryParameters();
							qp.setPositionalParameterTypes( new Type[] { addressPersister.getIdentifierType() } );
							qp.setPositionalParameterValues( new Object[] { a } );
							qp.setOptionalObject( a );
							qp.setOptionalEntityName( addressPersister.getEntityName() );
							qp.setOptionalId( a );
							qp.setLockOptions( LockOptions.NONE );
							return qp;
						}

					}
			);
			assertEquals( 1, results.size() );
			Object result = results.get( 0 );
			assertNotNull( result );
		}

		// test loading the Person (the entity with normal id def, but mixed composite fk to Address)
		{
			final List results = getResults(
					personPersister,
					new Callback() {
						@Override
						public void bind(PreparedStatement ps) throws SQLException {
							ps.setString( 1, personId );
						}

						@Override
						public QueryParameters getQueryParameters() {
							QueryParameters qp = new QueryParameters();
							qp.setPositionalParameterTypes( new Type[] { personPersister.getIdentifierType() } );
							qp.setPositionalParameterValues( new Object[] { personId } );
							qp.setOptionalObject( null );
							qp.setOptionalEntityName( personPersister.getEntityName() );
							qp.setOptionalId( personId );
							qp.setLockOptions( LockOptions.NONE );
							return qp;
						}

					}
			);
			assertEquals( 1, results.size() );
			Object result = results.get( 0 );
			assertNotNull( result );
		}

//		CardField cardFieldWork = ExtraAssertions.assertTyping( CardField.class, result );
//		assertEquals( cardFieldGotten, cardFieldWork );

		// clean up test data
		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Address" ).executeUpdate();
		s.createQuery( "delete Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	private List getResults(final EntityPersister entityPersister, final Callback callback) {
		final LoadPlan plan = Helper.INSTANCE.buildLoadPlan( sessionFactory(), entityPersister );

		final LoadQueryDetails queryDetails = Helper.INSTANCE.buildLoadQueryDetails( plan, sessionFactory() );
		final String sql = queryDetails.getSqlStatement();
		final ResultSetProcessor resultSetProcessor = queryDetails.getResultSetProcessor();

		final List results = new ArrayList();

		final Session workSession = openSession();
		workSession.beginTransaction();
		workSession.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						System.out.println( "SQL : " + sql );
						PreparedStatement ps = connection.prepareStatement( sql );
						callback.bind( ps );
						ResultSet resultSet = ps.executeQuery();
						//callback.beforeExtractResults( workSession );
						results.addAll(
								resultSetProcessor.extractResults(
										resultSet,
										(SessionImplementor) workSession,
										callback.getQueryParameters(),
										Helper.parameterContext(),
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
		workSession.getTransaction().commit();
		workSession.close();

		return results;
	}


	private interface Callback {
		void bind(PreparedStatement ps) throws SQLException;
		QueryParameters getQueryParameters();
	}
}
