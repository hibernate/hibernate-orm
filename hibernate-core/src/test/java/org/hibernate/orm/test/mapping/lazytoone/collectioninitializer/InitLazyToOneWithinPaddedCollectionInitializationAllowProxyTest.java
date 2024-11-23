/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.lazytoone.collectioninitializer;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.loader.BatchFetchStyle.PADDED;

/**
 * Test lazy-to-one initialization within a collection initialization,
 * with the PADDED batch-fetch-style.
 * <p>
 * In particular, with Offer having a lazy to-one association to CostCenter,
 * and User having a lazy to-many association to UserAuthorization1 and UserAuthorization2,
 * and UserAuthorization1 and UserAuthorization2 having an EAGER association to CostCenter,
 * test:
 * <ul>
 *     <li>Get a reference to Offer (which will create an uninitialized proxy for CostCenter)</li>
 *     <li>Get a reference to User</li>
 *     <li>Initialize User's collection containing UserAuthorization1 and UserAuthorization2,
 *     which will initialize CostCenter DURING the loading,
 *     which used to fail because we tried to initialize CostCenter twice
 *     (once for UserAuthorization1, and once for UserAuthorization2)</li>
 * </ul>
 */
@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue(jiraKey = "HHH-14730")
public class InitLazyToOneWithinPaddedCollectionInitializationAllowProxyTest
		extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( User.class );
		sources.addAnnotatedClass( UserAuthorization.class );
		sources.addAnnotatedClass( Company.class );
		sources.addAnnotatedClass( CostCenter.class );
		sources.addAnnotatedClass( Offer.class );
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, true );
		ssrb.applySetting( AvailableSettings.BATCH_FETCH_STYLE, PADDED );
		ssrb.applySetting( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, 10 );
	}

	@Override
	protected void afterSessionFactoryBuilt(SessionFactoryImplementor sessionFactory) {
		inTransaction( session -> {
			User user0 = new User();
			user0.setId( 0L );
			session.persist( user0 );

			User user1 = new User();
			user1.setId( 1L );
			session.persist( user1 );

			User user2 = new User();
			user2.setId( 2L );
			session.persist( user2 );

			Company company = new Company();
			company.setId( 2L );
			session.persist( company );

			CostCenter costCenter = new CostCenter();
			costCenter.setId( 3L );
			costCenter.setCompany( company );
			session.persist( costCenter );

			UserAuthorization user0Authorization1 = new UserAuthorization();
			user0Authorization1.setId( 1L );
			user0Authorization1.setUser( user0 );
			user0Authorization1.setCostCenter( costCenter );
			session.persist( user0Authorization1 );

			UserAuthorization user1Authorization1 = new UserAuthorization();
			user1Authorization1.setId( 11L );
			user1Authorization1.setUser( user1 );
			user1Authorization1.setCostCenter( costCenter );
			session.persist( user1Authorization1 );

			UserAuthorization user1Authorization2 = new UserAuthorization();
			user1Authorization2.setId( 12L );
			user1Authorization2.setUser( user1 );
			user1Authorization2.setCostCenter( costCenter );
			session.persist( user1Authorization2 );

			UserAuthorization user2Authorization1 = new UserAuthorization();
			user2Authorization1.setId( 21L );
			user2Authorization1.setUser( user2 );
			user2Authorization1.setCostCenter( costCenter );
			session.persist( user2Authorization1 );

			UserAuthorization user2Authorization2 = new UserAuthorization();
			user2Authorization2.setId( 22L );
			user2Authorization2.setUser( user2 );
			user2Authorization2.setCostCenter( costCenter );
			session.persist( user2Authorization2 );

			UserAuthorization user2Authorization3 = new UserAuthorization();
			user2Authorization3.setId( 23L );
			user2Authorization3.setUser( user2 );
			user2Authorization3.setCostCenter( costCenter );
			session.persist( user2Authorization3 );

			Offer offer = new Offer();
			offer.setId( 6L );
			offer.setCostCenter( costCenter );
			session.persist( offer );
		} );
	}

	@Test
	public void testOneReference() {
		inTransaction( (session) -> {
			// Add a lazy proxy of the cost center to the persistence context
			// through the lazy to-one association from the offer.
			Offer offer = session.find( Offer.class, 6L );

			User user = session.find( User.class, 0L );

			assertThat( Hibernate.isInitialized( offer.getCostCenter() ) ).isFalse();

			// Trigger lazy-loading of the cost center
			// through the loading of the authorization,
			// which contains an eager reference to the cost center.
			assertThat( user.getAuthorizations().size() ).isEqualTo( 1 );

			assertThat( Hibernate.isInitialized( offer.getCostCenter() ) ).isTrue();
		} );
	}

	@Test
	public void testTwoReferences() {
		inTransaction( (session) -> {
			// Add a lazy proxy of the cost center to the persistence context
			// through the lazy to-one association from the offer.
			Offer offer = session.find( Offer.class, 6L );

			User user = session.find( User.class, 1L );

			assertThat( Hibernate.isInitialized( offer.getCostCenter() ) ).isFalse();

			// Trigger lazy-loading of the cost center
			// through the loading of the 2 authorizations,
			// which both contain an eager reference to the cost center.
			assertThat( user.getAuthorizations().size() ).isEqualTo( 2 );

			assertThat( Hibernate.isInitialized( offer.getCostCenter() ) ).isTrue();
		} );
	}

	@Test
	public void testThreeReferences() {
		inTransaction( (session) -> {
			// Add a lazy proxy of the cost center to the persistence context
			// through the lazy to-one association from the offer.
			Offer offer = session.find( Offer.class, 6L );

			User user = session.find( User.class, 2L );

			assertThat( Hibernate.isInitialized( offer.getCostCenter() ) ).isFalse();

			// Trigger lazy-loading of the cost center
			// through the loading of the 3 authorizations,
			// which all contain an eager reference to the cost center.
			assertThat( user.getAuthorizations().size() ).isEqualTo( 3 );

			assertThat( Hibernate.isInitialized( offer.getCostCenter() ) ).isTrue();
		} );
	}
}
