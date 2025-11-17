/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.SessionImpl;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/proxy/DataPoint.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0")
)
public class ProxyTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testFinalizeFiltered(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataPoint dp = new DataPoint();
					dp.setDescription( "a data point" );
					dp.setX( new BigDecimal( "1.0" ) );
					dp.setY( new BigDecimal( "2.0" ) );
					session.persist( dp );
					session.flush();
					session.clear();

					DataPoint d = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( d ) ).isFalse();

					assertThrows( NoSuchMethodException.class, () ->
							d.getClass().getDeclaredMethod( "finalize", (Class[]) null )
					);

					session.remove( d );
				}
		);
	}

	@Test
	public void testProxyException(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataPoint dp = new DataPoint();
					dp.setDescription( "a data point" );
					dp.setX( new BigDecimal( "1.0" ) );
					dp.setY( new BigDecimal( "2.0" ) );
					session.persist( dp );
					session.flush();
					session.clear();

					DataPoint d = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( d ) ).isFalse();

					assertThrows( Exception.class, () -> d.exception() );

					session.remove( d );
				}
		);
	}

	@Test
	public void testProxyExceptionWithNewGetReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataPoint dp = new DataPoint();
					dp.setDescription( "a data point" );
					dp.setX( new BigDecimal( "1.0" ) );
					dp.setY( new BigDecimal( "2.0" ) );
					session.persist( dp );
					session.flush();
					session.clear();

					DataPoint d = session.getReference( dp );
					assertThat( Hibernate.isInitialized( d ) ).isFalse();

					assertThrows( Exception.class, () -> d.exception() );

					session.remove( d );
				}
		);
	}

	@Test
	public void testProxyExceptionWithOldGetReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataPoint dp = new DataPoint();
					dp.setDescription( "a data point" );
					dp.setX( new BigDecimal( "1.0" ) );
					dp.setY( new BigDecimal( "2.0" ) );
					session.persist( dp );
					session.flush();
					session.clear();

					DataPoint d = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( d ) ).isFalse();

					assertThrows( Exception.class, () -> d.exception() );

					session.remove( d );
				}
		);
	}

	@Test
	public void testProxySerializationAfterSessionClosed(SessionFactoryScope scope) {
		DataPoint d = scope.fromTransaction(
				session -> {
					DataPoint dp = new DataPoint();
					dp.setDescription( "a data point" );
					dp.setX( new BigDecimal( "1.0" ) );
					dp.setY( new BigDecimal( "2.0" ) );
					session.persist( dp );
					session.flush();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					return dp;
				}
		);
		SerializationHelper.clone( d );

		scope.inTransaction( session -> session.remove( d ) );
	}

	@Test
	public void testInitializedProxySerializationAfterSessionClosed(SessionFactoryScope scope) {
		DataPoint d = scope.fromTransaction(
				session -> {
					DataPoint dp = new DataPoint();
					dp.setDescription( "a data point" );
					dp.setX( new BigDecimal( "1.0" ) );
					dp.setY( new BigDecimal( "2.0" ) );
					session.persist( dp );
					session.flush();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					Hibernate.initialize( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isTrue();
					return dp;
				}
		);
		SerializationHelper.clone( d );

		scope.inTransaction( session -> session.remove( d ) );
	}

	@Test
	public void testProxySerialization(SessionFactoryScope scope) {
		Session s = scope.getSessionFactory().openSession();
		Session sclone = null;
		try {
			s.beginTransaction();
			DataPoint dp = new DataPoint();
			Object none = null;
			try {
				dp.setDescription( "a data point" );
				dp.setX( new BigDecimal( "1.0" ) );
				dp.setY( new BigDecimal( "2.0" ) );
				s.persist( dp );
				s.flush();
				s.clear();

				dp = s.getReference( DataPoint.class, dp.getId() );
				assertThat( Hibernate.isInitialized( dp ) ).isFalse();
				dp.getId();
				assertThat( Hibernate.isInitialized( dp ) ).isFalse();
				dp.getDescription();
				assertThat( Hibernate.isInitialized( dp ) ).isTrue();
				none = s.getReference( DataPoint.class, 666L );

				assertThat( Hibernate.isInitialized( none ) ).isFalse();

				s.getTransaction().commit();
			}
			finally {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
			}
			s.unwrap( SessionImplementor.class ).getJdbcCoordinator().getLogicalConnection().manualDisconnect();

			Object[] holder = new Object[] {s, dp, none};

			holder = (Object[]) SerializationHelper.clone( holder );
			sclone = (Session) holder[0];
			dp = (DataPoint) holder[1];
			none = holder[2];

			//close the original:
			s.close();
			try {
				sclone.beginTransaction();

				DataPoint sdp = sclone.getReference( DataPoint.class, dp.getId() );
				assertThat( sdp ).isSameAs( dp );
				assertThat( sdp ).isNotInstanceOf( HibernateProxy.class );
				Object snone = sclone.getReference( DataPoint.class, 666L );
				assertThat( snone ).isSameAs( none );
				assertThat( snone ).isInstanceOf( HibernateProxy.class );

				sclone.remove( dp );

				sclone.getTransaction().commit();
			}
			finally {
				if ( sclone.getTransaction().isActive() ) {
					sclone.getTransaction().rollback();
				}
			}
		}
		finally {
			if ( s.isOpen() ) {
				s.close();
			}
			if ( sclone != null && sclone.isOpen() ) {
				sclone.close();
			}
		}

	}

	@Test
	public void testProxy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataPoint dp = new DataPoint();
					dp.setDescription( "a data point" );
					dp.setX( new BigDecimal( "1.0" ) );
					dp.setY( new BigDecimal( "2.0" ) );
					session.persist( dp );
					session.flush();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					DataPoint dp2 = session.get( DataPoint.class, dp.getId() );
					assertThat( dp2 ).isSameAs( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isTrue();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					dp2 = session.getReference( DataPoint.class, dp.getId() );
					assertThat( dp2 ).isSameAs( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					dp2 = session.get( DataPoint.class, dp.getId(), LockMode.READ );
					assertThat( dp2 ).isSameAs( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isTrue();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					dp2 = session.find( DataPoint.class, dp.getId(), LockMode.READ );
					assertThat( dp2 ).isSameAs( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isTrue();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					dp2 = session.createQuery( "from DataPoint", DataPoint.class ).uniqueResult();
					assertThat( dp2 ).isSameAs( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isTrue();
					session.remove( dp );
				}
		);
	}

	@Test
	public void testProxyWithGetReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataPoint dp = new DataPoint();
					dp.setDescription( "a data point" );
					dp.setX( new BigDecimal( "1.0" ) );
					dp.setY( new BigDecimal( "2.0" ) );
					session.persist( dp );
					session.flush();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					DataPoint dp2 = session.get( DataPoint.class, dp.getId() );
					assertThat( dp2 ).isSameAs( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isTrue();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					dp2 = session.getReference( DataPoint.class, dp.getId() );
					assertThat( dp2 ).isSameAs( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					session.clear();

					dp = session.getReference( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					dp2 = session.getReference( dp );
					assertThat( dp2 ).isSameAs( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					dp2 = session.find( DataPoint.class, dp.getId(), LockMode.READ );
					assertThat( dp2 ).isSameAs( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isTrue();
					session.clear();

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					dp2 = session.createQuery( "from DataPoint", DataPoint.class ).uniqueResult();
					assertThat( dp2 ).isSameAs( dp );
					assertThat( Hibernate.isInitialized( dp ) ).isTrue();
					session.remove( dp );
				}
		);
	}

	@Test
	public void testSubsequentNonExistentProxyAccess(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					DataPoint proxy = session.getReference( DataPoint.class, (long) -1 );
					assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
					try {
						proxy.getDescription();
						fail( "proxy access did not fail on non-existent proxy" );
					}
					catch (ObjectNotFoundException onfe) {
						// expected
					}
					catch (Throwable e) {
						fail( "unexpected exception type on non-existent proxy access : " + e );
					}
					// try it a second (subsequent) time...
					try {
						proxy.getDescription();
						fail( "proxy access did not fail on non-existent proxy" );
					}
					catch (ObjectNotFoundException onfe) {
						// expected
					}
					catch (Throwable e) {
						fail( "unexpected exception type on non-existent proxy access : " + e );
					}
				}
		);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testProxyEviction(SessionFactoryScope scope) {
		Container container = new Container( "container" );
		scope.inTransaction(
				session -> {
					container.setOwner( new Owner( "owner" ) );
					container.setInfo( new Info( "blah blah blah" ) );
					container.getDataPoints()
							.add( new DataPoint( new BigDecimal( 1 ), new BigDecimal( 1 ), "first data point" ) );
					container.getDataPoints()
							.add( new DataPoint( new BigDecimal( 2 ), new BigDecimal( 2 ), "second data point" ) );
					session.persist( container );
				}
		);

		scope.inTransaction(
				session -> {
					Container c = session.getReference( Container.class, container.getId() );
					assertThat( Hibernate.isInitialized( c ) ).isFalse();
					session.evict( c );
					try {
						c.getName();
						fail( "expecting LazyInitializationException" );
					}
					catch (LazyInitializationException e) {
						// expected result
					}

					c = session.getReference( Container.class, container.getId() );
					assertThat( Hibernate.isInitialized( c ) ).isFalse();
					Info i = c.getInfo();
					assertThat( Hibernate.isInitialized( c ) ).isTrue();
					assertThat( Hibernate.isInitialized( i ) ).isFalse();
					session.evict( c );
					try {
						i.getDetails();
						fail( "expecting LazyInitializationException" );
					}
					catch (LazyInitializationException e) {
						// expected result
					}

					session.remove( c );
				}
		);
	}

	@Test
	public void testFullyLoadedPCSerialization(SessionFactoryScope scope) {
		int containerCount = 10;
		int nestedDataPointCount = 5;
		Long lastContainerId = scope.fromTransaction(
				session -> {
					Long last = 0L;
					for ( int c_indx = 0; c_indx < containerCount; c_indx++ ) {
						Owner owner = new Owner( "Owner #" + c_indx );
						Container container = new Container( "Container #" + c_indx );
						container.setOwner( owner );
						for ( int dp_indx = 0; dp_indx < nestedDataPointCount; dp_indx++ ) {
							DataPoint dp = new DataPoint();
							dp.setDescription( "data-point [" + c_indx + ", " + dp_indx + "]" );
							// more HSQLDB fun...
							//				dp.setX( new BigDecimal( c_indx ) );
							dp.setX( new BigDecimal( c_indx + dp_indx ) );
							dp.setY( new BigDecimal( dp_indx ) );
							container.getDataPoints().add( dp );
						}
						session.persist( container );
						last =  container.getId();
					}
					return last;
				}
		);

		scope.inSession(
				session -> {
					session.setHibernateFlushMode( FlushMode.MANUAL );
					try {
						session.beginTransaction();
						// load the last container as a proxy
						Container proxy = session.getReference( Container.class, lastContainerId );
						assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
						// load the rest back into the PC
						List<Container> all = session.createQuery(
										"from Container as c inner join fetch c.owner inner join fetch c.dataPoints where c.id <> :l",
										Container.class )
								.setParameter( "l", lastContainerId )
								.list();
						Container container = (Container) all.get( 0 );
						session.remove( container );
						// force a snapshot retrieval of the proxied container
						SessionImpl sImpl = (SessionImpl) session;
						sImpl.getPersistenceContext().getDatabaseSnapshot(
								lastContainerId,
								sImpl.getFactory().getMappingMetamodel()
										.getEntityDescriptor( Container.class.getName() )
						);
						assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
						session.getTransaction().commit();
						byte[] bytes = SerializationHelper.serialize( session );
						SerializationHelper.deserialize( bytes );

						session.beginTransaction();
						int count = session.createMutationQuery( "delete DataPoint" ).executeUpdate();
						assertThat( count )
								.describedAs( "unexpected DP delete count" )
								.isEqualTo( containerCount * nestedDataPointCount );
						count = session.createMutationQuery( "delete Container" ).executeUpdate();
						assertThat( count )
								.describedAs( "unexpected container delete count" )
								.isEqualTo( containerCount );
						count = session.createMutationQuery( "delete Owner" ).executeUpdate();
						assertThat( count )
								.describedAs( "unexpected owner delete count" )
								.isEqualTo( containerCount );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testRefreshLockInitializedProxy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataPoint dp = newPersistentDataPoint( session );

					dp = session.getReference( DataPoint.class, dp.getId() );
					dp.getX();
					assertThat( Hibernate.isInitialized( dp ) ).isTrue();

					session.refresh( dp, LockMode.PESSIMISTIC_WRITE );
					assertThat( session.getCurrentLockMode( dp ) ).isSameAs( LockMode.PESSIMISTIC_WRITE );

					session.remove( dp );
				}
		);
	}

	@Test
	@JiraKey("HHH-1645")
	public void testRefreshLockUninitializedProxy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataPoint dp = newPersistentDataPoint( session );

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();

					session.refresh( dp, LockMode.PESSIMISTIC_WRITE );
					assertThat( session.getCurrentLockMode( dp ) ).isSameAs( LockMode.PESSIMISTIC_WRITE );

					session.remove( dp );
				}
		);
	}

	private static DataPoint newPersistentDataPoint(Session s) {
		DataPoint dp = new DataPoint();
		dp.setDescription( "a data point" );
		dp.setX( new BigDecimal( "1.0" ) );
		dp.setY( new BigDecimal( "2.0" ) );
		s.persist( dp );
		s.flush();
		s.clear();
		return dp;
	}

	@Test
	@JiraKey("HHH-1645")
	public void testRefreshLockUninitializedProxyThenRead(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataPoint dp = newPersistentDataPoint( session );

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					session.refresh( dp, LockMode.PESSIMISTIC_WRITE );
					dp.getX();
					assertThat( session.getCurrentLockMode( dp ) ).isSameAs( LockMode.PESSIMISTIC_WRITE );

					session.remove( dp );
				}
		);
	}

	@Test
	public void testLockUninitializedProxy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataPoint dp = newPersistentDataPoint( session );

					dp = session.getReference( DataPoint.class, dp.getId() );
					assertThat( Hibernate.isInitialized( dp ) ).isFalse();
					session.lock( dp, LockMode.PESSIMISTIC_WRITE );
					assertThat( session.getCurrentLockMode( dp ) ).isSameAs( LockMode.PESSIMISTIC_WRITE );

					session.remove( dp );
				}
		);
	}
}
