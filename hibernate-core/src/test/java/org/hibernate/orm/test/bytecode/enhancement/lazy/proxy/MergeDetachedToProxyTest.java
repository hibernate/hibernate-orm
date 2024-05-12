/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey( "HHH-11147" )
@DomainModel(
		annotatedClasses = {
				MergeDetachedToProxyTest.AEntity.class,
				MergeDetachedToProxyTest.BEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class MergeDetachedToProxyTest {

	@Test
	public void testMergeInitializedDetachedOntoProxy(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		final AEntity aEntityDetached = scope.fromTransaction(
				session -> {
					AEntity aEntity = session.get( AEntity.class, 1 );
					assertIsEnhancedProxy( aEntity.bEntity );
					Hibernate.initialize( aEntity.bEntity );
					return aEntity;
				}
		);

		statistics.clear();
		assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 0L );

		scope.inSession(
				session -> {
					BEntity bEntity = session.getReference( BEntity.class, 2 );
					assertIsEnhancedProxy( bEntity );
					assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 0L );

					AEntity aEntityMerged = (AEntity) session.merge( aEntityDetached );
					assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 1L );

					assertSame( bEntity, aEntityMerged.bEntity );
					assertEquals( "a description", aEntityDetached.bEntity.description );
					assertTrue( Hibernate.isInitialized( bEntity ) );
				}
		);

		assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 1L );
	}

	@Test
	public void testMergeUpdatedDetachedOntoProxy(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		final AEntity aEntityDetached = scope.fromTransaction(
				session -> {
					AEntity aEntity = session.get( AEntity.class, 1 );
					assertIsEnhancedProxy( aEntity.bEntity );
					Hibernate.initialize( aEntity.bEntity );
					return aEntity;
				}
		);

		aEntityDetached.bEntity.description = "new description";

		statistics.clear();
		assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 0L );

		scope.inSession(
				session -> {
					BEntity bEntity = session.getReference( BEntity.class, 2 );
					assertIsEnhancedProxy( bEntity );
					assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 0L );

					AEntity aEntityMerged = (AEntity) session.merge( aEntityDetached );
					assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 1L );

					assertSame( bEntity, aEntityMerged.bEntity );
					assertEquals( "new description", aEntityDetached.bEntity.description );
					assertTrue( Hibernate.isInitialized( bEntity ) );
				}
		);

		assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 1L );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final AEntity aEntity = new AEntity();
					aEntity.id = 1;
					final BEntity bEntity = new BEntity();
					bEntity.id = 2;
					bEntity.description = "a description";
					aEntity.bEntity = bEntity;
					session.persist( aEntity );
				}
		);
	}

	@AfterEach
	public void clearTestData(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from AEntity" ).executeUpdate();
					session.createQuery( "delete from BEntity" ).executeUpdate();
				}
		);
	}

	private void assertIsEnhancedProxy(Object entity) {
		assertTrue( PersistentAttributeInterceptable.class.isInstance( entity ) );

		final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) entity;
		final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
		assertTrue( EnhancementAsProxyLazinessInterceptor.class.isInstance( interceptor ) );
	}

	@Entity(name = "AEntity")
	public static class AEntity {
		@Id
		private int id;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private BEntity bEntity;
	}

	@Entity(name = "BEntity")
	public static class BEntity {
		@Id
		private int id;

		private String description;

	}
}
