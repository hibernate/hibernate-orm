/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.util.HashSet;
import java.util.List;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@JiraKey("HHH14424")
@DomainModel(
		annotatedClasses = {
				SamplingOrder.class,
				Customer.class,
				User.class,
				Role.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "100" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
public class LoadAndUpdateEntitiesWithCollectionsTest {

	@BeforeAll
	static void beforeAll() {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		assumeFalse( byteCodeProvider != null && !BytecodeProviderInitiator.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals(
				byteCodeProvider ) );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User user = new User();
					user.setEmail( "foo@bar.com" );

					Role role = new Role();
					role.setName( "admin" );

					user.addRole( role );

					Customer customer = new Customer();
					customer.setUser( user );

					SamplingOrder order = new SamplingOrder();
					order.setNote( "it is a sample" );
					order.setCustomer( customer );


					session.persist( user );
					session.persist( role );
					session.persist( customer );
					session.persist( order );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					CriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<SamplingOrder> cq = cb.createQuery( SamplingOrder.class );
					Root<SamplingOrder> root = cq.from( SamplingOrder.class );
					root.fetch( SamplingOrder_.customer );

					TypedQuery<SamplingOrder> query = session.createQuery( cq );
					query.getResultList();
				}
		);

		scope.inTransaction(
				session -> {
					List<User> users = session.createQuery( "from User u", User.class ).list();
					User user = users.get( 0 );
					assertThat( user.getEmail(), is( "foo@bar.com" ) );
				}
		);
	}

	@Test
	public void testAddUserRoles(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SamplingOrder samplingOrder = getSamplingOrder( session );
					User user = samplingOrder.getCustomer().getUser();
					Role role = new Role();
					role.setName( "superuser" );
					user.addRole( role );
					session.persist( role );
				}
		);

		scope.inTransaction(
				session -> {
					List<User> users = session.createQuery( "from User u", User.class ).list();
					User user = users.get( 0 );
					assertThat( user.getEmail(), is( "foo@bar.com" ) );
					assertThat( user.getRoles().size(), is( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					SamplingOrder samplingOrder = getSamplingOrder( session );
					User user = samplingOrder.getCustomer().getUser();
					Role role = new Role();
					role.setName( "user" );
					user.getRoles().add( role );
					session.persist( role );
				}
		);

		scope.inTransaction(
				session -> {
					List<User> users = session.createQuery( "from User u", User.class ).list();
					User user = users.get( 0 );
					assertThat( user.getEmail(), is( "foo@bar.com" ) );
					assertThat( user.getRoles().size(), is( 3 ) );
				}
		);

		scope.inTransaction(
				session -> {
					User user = session
							.createQuery(
									"from User",
									User.class
							)
							.list()
							.get( 0 );
					Role role = new Role();
					user.getRoles().add( role );
					session.persist( role );
				}
		);

		scope.inTransaction(
				session -> {
					List<User> users = session
							.createQuery(
									"from User u",
									User.class
							)
							.list();
					User user = users
							.get( 0 );
					assertThat( user.getEmail(), is( "foo@bar.com" ) );
					assertThat( user.getRoles().size(), is( 4 ) );
				}
		);

	}

	@Test
	public void testDeleteUserRoles(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SamplingOrder samplingOrder = getSamplingOrder( session );
					User user = samplingOrder.getCustomer().getUser();
					user.setRoles( new HashSet<>() );
				}
		);

		scope.inTransaction(
				session -> {
					List<User> users = session.createQuery( "from User u", User.class ).list();
					User user = users.get( 0 );
					assertThat( user.getEmail(), is( "foo@bar.com" ) );
					assertThat( user.getRoles().size(), is( 0 ) );
				}
		);
	}

	@Test
	public void testModifyUserMail(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SamplingOrder samplingOrder = getSamplingOrder( session );
					User user = samplingOrder.getCustomer().getUser();
					user.setEmail( "bar@foo.com" );
				}
		);

		scope.inTransaction(
				session -> {
					List<User> users = session.createQuery( "from User u", User.class ).list();
					User user = users.get( 0 );
					assertThat( user.getEmail(), is( "bar@foo.com" ) );
					assertThat( user.getRoles().size(), is( 1 ) );
				}
		);
	}

	private SamplingOrder getSamplingOrder(SessionImplementor session) {
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<SamplingOrder> cq = cb.createQuery( SamplingOrder.class );
		Root<SamplingOrder> root = cq.from( SamplingOrder.class );
		root.fetch( SamplingOrder_.customer );

		TypedQuery<SamplingOrder> query = session.createQuery( cq );
		List<SamplingOrder> resultList = query.getResultList();
		return resultList.get( 0 );
	}
}
