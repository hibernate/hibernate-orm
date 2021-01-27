/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance;

import javax.persistence.PersistenceException;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadata;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hibernate.cfg.AvailableSettings.GENERATE_STATISTICS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry( settings = @Setting( name= GENERATE_STATISTICS, value = "true" ) )
@DomainModel( annotatedClasses = { Principal.class, User.class } )
@SessionFactory
public class InheritedNaturalIdTest {
	@Test
	@TestForIssue( jiraKey = "HHH-10360")
	public void verifyMappingModel(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityMappingType userMapping = sessionFactory.getRuntimeMetamodels().getEntityMappingType( User.class );

		final SingularAttributeMapping uidMapping = ((SimpleNaturalIdMapping) userMapping.getNaturalIdMapping()).getAttribute();
		assertThat( uidMapping.getAttributeName(), is ("uid" ) );
		final StateArrayContributorMetadata uidMetadata = uidMapping.getAttributeMetadataAccess().resolveAttributeMetadata( null );
		assertThat( uidMetadata.isNullable(), is( true ) );

		final EntityPersister rootEntityPersister = userMapping.getEntityPersister();
		final int uidLegacyPropertyIndex = rootEntityPersister.getEntityMetamodel().getPropertyIndex( "uid" );
		assertThat( uidLegacyPropertyIndex, is ( 0 ) );
		assertThat( rootEntityPersister.getPropertyNullability()[ uidLegacyPropertyIndex ], is( true ) );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.save( new User( ORIGINAL ) )
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "delete Principal" ).executeUpdate()
		);
	}

	public static final String ORIGINAL = "steve";
	public static final String UPDATED = "sebersole";

	@Test
	public void testNaturalIdApi(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Principal principalBySimple = session.bySimpleNaturalId( Principal.class ).load( ORIGINAL );
					assertThat( principalBySimple, notNullValue() );

					final Principal principal = session.byNaturalId( Principal.class ).using( "uid", ORIGINAL ).load();
					assertThat( principal, notNullValue() );

					final Principal userBySimple = session.bySimpleNaturalId( User.class ).load( ORIGINAL );
					assertThat( userBySimple, notNullValue() );

					final Principal user = session.byNaturalId( User.class ).using( "uid", ORIGINAL ).load();
					assertThat( user, notNullValue() );

					assertThat( principalBySimple, is( principal ) );
					assertThat( principalBySimple, is( userBySimple ) );
					assertThat( principalBySimple, is( user ) );
				}
		);
	}


	@Test
	@FailureExpected(
			reason = "Do not believe this is a valid test.  The natural-id is explicitly defined as mutable " +
					"and then the test checks that it is not mutable"
	)
	public void testSubclassModifiableNaturalId(SessionFactoryScope scope) {
		// todo (6.0) : I'm not understanding this test.. the `User` natural-id is defined as mutable
		//		- why would changing the value make the process "blow up"?
		scope.inTransaction(
				(session) -> {
					final User user = session.bySimpleNaturalId( User.class ).load( ORIGINAL );
					assertNotNull( user );

					// change the natural id - the flush should blow up
					user.setUid( UPDATED );
					try {
						session.flush();
						fail();
					}
					catch (PersistenceException e) {
						assertThat( e.getMessage(), containsString( "An immutable natural identifier" ) );
						assertThat( e.getMessage(), containsString( "was altered" ) );
					}
					finally {
						// force the Session to close
						session.close();
					}
				}
		);
	}

	@Test
	public void testSubclassDeleteNaturalId(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Principal p = session.bySimpleNaturalId( Principal.class ).load( ORIGINAL );
					assertNotNull( p );

					session.delete( p );
					session.flush();
				}
		);

		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> {
					final Principal p = session.bySimpleNaturalId( Principal.class ).load( ORIGINAL );
					assertThat( p, nullValue() );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}
}
