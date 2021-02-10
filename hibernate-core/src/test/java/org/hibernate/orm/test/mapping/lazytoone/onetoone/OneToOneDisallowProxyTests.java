/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.lazytoone.onetoone;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javax.persistence.FetchType.LAZY;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@RunWith( BytecodeEnhancerRunner.class)
@EnhancementOptions( lazyLoading = true )
public class OneToOneDisallowProxyTests extends BaseNonConfigCoreFunctionalTestCase {
	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( Customer.class );
		sources.addAnnotatedClass( SupplementalInfo.class );
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, false );
		sqlStatementInterceptor = new SQLStatementInterceptor( ssrb );
	}

	@Test
	public void testOwnerIsProxy() {
		sqlStatementInterceptor.clear();

		inTransaction(
				(session) -> {
					final SupplementalInfo supplementalInfo = session.byId( SupplementalInfo.class ).getReference( 1 );
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 0 ) );
					assertThat( supplementalInfo, instanceOf( HibernateProxy.class ) );
					assertThat( Hibernate.isInitialized( supplementalInfo ), is( false ) );

					// access the id - should do nothing with db
					supplementalInfo.getId();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 0 ) );
					assertThat( Hibernate.isInitialized( supplementalInfo ), is( false ) );

					// this should initialize the proxy
					supplementalInfo.getSomething();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 1 ) );
					assertThat( Hibernate.isInitialized( supplementalInfo ), is( true ) );

					assertThat( Hibernate.isPropertyInitialized( supplementalInfo, "customer" ), is( true ) );

					// should not trigger a load and the `customer` reference should be an uninitialized enhanced proxy
					final Customer customer = supplementalInfo.getCustomer();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 1 ) );
					assertThat( customer, instanceOf( HibernateProxy.class ) );
					assertThat( Hibernate.isInitialized( customer ), is( false ) );

					// should trigger no loads
					customer.getId();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 1 ) );
					assertThat( Hibernate.isInitialized( customer ), is( false ) );

					customer.getName();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 2 ) );
					assertThat( Hibernate.isInitialized( customer ), is( true ) );
				}
		);
	}

	@Before
	public void createTestData() {
		inTransaction(
				(session) -> {
					final Customer customer = new Customer( 1, "Acme Brick" );
					session.persist( customer );
					final SupplementalInfo supplementalInfo = new SupplementalInfo( 1, customer, "extra details" );
					session.persist( supplementalInfo );
				}
		);
	}

	@After
	public void dropTestData() {
		inTransaction(
				(session) -> {
					session.createQuery( "delete SupplementalInfo" ).executeUpdate();
					session.createQuery( "delete Customer" ).executeUpdate();
				}
		);
	}

	@Entity( name = "Customer" )
	@Table( name = "customer" )
	public static class Customer {
		@Id
		private Integer id;
		private String name;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		private void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "SupplementalInfo" )
	@Table( name = "supplemental" )
	public static class SupplementalInfo {
		@Id
		private Integer id;

		@OneToOne( fetch = LAZY, optional = false )
		//@LazyToOne( value = NO_PROXY )
		private Customer customer;

		private String something;

		public SupplementalInfo() {
		}

		public SupplementalInfo(Integer id, Customer customer, String something) {
			this.id = id;
			this.customer = customer;
			this.something = something;
		}

		public Integer getId() {
			return id;
		}

		private void setId(Integer id) {
			this.id = id;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public String getSomething() {
			return something;
		}

		public void setSomething(String something) {
			this.something = something;
		}
	}
}
