/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Tests that {@link PostUpdateEventListener}
 * work correctly for every type of property (single-valued associations and @Basic).
 */
public class PostUpdateEventListenerTest extends BaseCoreFunctionalTestCase {

	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private PostUpdateEventListener listenerMock;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyMappedSuperclass.class, MyEntity.class, MyOtherEntity.class, MyEmbeddable.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setImplicitNamingStrategy( ImplicitNamingStrategyComponentPathImpl.INSTANCE );
	}

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
		listenerMock = mock( PostUpdateEventListener.class );
		when( listenerMock.requiresPostCommitHandling( any() ) ).thenReturn( false );
		builder.applyIntegrator(
				new Integrator() {
					@Override
					public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
							SessionFactoryServiceRegistry serviceRegistry) {
						integrate( serviceRegistry );
					}

					private void integrate(SessionFactoryServiceRegistry serviceRegistry) {
						serviceRegistry.getService( EventListenerRegistry.class )
								.getEventListenerGroup( EventType.POST_UPDATE ).appendListener( listenerMock );
					}

					@Override
					public void disintegrate(SessionFactoryImplementor sessionFactory,
							SessionFactoryServiceRegistry serviceRegistry) {
					}
				}
		);
	}

	@Override
	protected void afterSessionFactoryBuilt() {
		inTransaction( s -> {
			MyEntity myEntity = new MyEntity();
			myEntity.setId( 1L );
			s.persist( myEntity );
			MyOtherEntity myOtherEntity1 = new MyOtherEntity();
			myOtherEntity1.setId( 1L );
			s.persist( myOtherEntity1 );
			MyOtherEntity myOtherEntity2 = new MyOtherEntity();
			myOtherEntity2.setId( 2L );
			s.persist( myOtherEntity2 );
		} );
	}

	@Test
	public void basic() {
		// null to non-null
		testUpdate( myEntity -> myEntity.setBasic( "initial" ), "basic" );

		// non-null to non-null
		testUpdate( myEntity -> myEntity.setBasic( "updated" ), "basic" );

		// non-null to null
		testUpdate( myEntity -> myEntity.setBasic( null ), "basic" );
	}

	@Test
	public void embedded_basic() {
		// null to non-null
		testUpdate( myEntity -> myEntity.getEmbedded().setBasic( "initial" ), "embedded" );

		// non-null to non-null
		testUpdate( myEntity -> myEntity.getEmbedded().setBasic( "updated" ), "embedded" );

		// non-null to null
		testUpdate( myEntity -> myEntity.getEmbedded().setBasic( null ), "embedded" );
	}

	@Test
	public void oneToOne_owningSide() {
		// null to non-null
		testUpdate( (s, myEntity) -> {
			MyOtherEntity other = s.get( MyOtherEntity.class, 1L );
			myEntity.setOneToOneOwningSide( other );
			other.setOneToOneNonOwningSide( myEntity );
		}, "oneToOneOwningSide" );

		// non-null to non-null
		testUpdate( (s, myEntity) -> {
			myEntity.getOneToOneOwningSide().setOneToOneNonOwningSide( null );
			MyOtherEntity other = s.get( MyOtherEntity.class, 2L );
			myEntity.setOneToOneOwningSide( other );
			other.setOneToOneNonOwningSide( myEntity );
		}, "oneToOneOwningSide" );

		// non-null to null
		testUpdate( (s, myEntity) -> {
			myEntity.getOneToOneOwningSide().setOneToOneNonOwningSide( null );
			myEntity.setOneToOneOwningSide( null );
		}, "oneToOneOwningSide" );
	}

	@Test
	public void embedded_oneToOne_owningSide() {
		// null to non-null
		testUpdate( (s, myEntity) -> {
			MyOtherEntity other = s.get( MyOtherEntity.class, 1L );
			myEntity.getEmbedded().setOneToOneOwningSide( other );
			other.setEmbeddedOneToOneNonOwningSide( myEntity );
		}, "embedded" );

		// non-null to non-null
		testUpdate( (s, myEntity) -> {
			myEntity.getEmbedded().getOneToOneOwningSide().setEmbeddedOneToOneNonOwningSide( null );
			MyOtherEntity other = s.get( MyOtherEntity.class, 2L );
			myEntity.getEmbedded().setOneToOneOwningSide( other );
			other.setEmbeddedOneToOneNonOwningSide( myEntity );
		}, "embedded" );

		// non-null to null
		testUpdate( (s, myEntity) -> {
			myEntity.getEmbedded().getOneToOneOwningSide().setEmbeddedOneToOneNonOwningSide( null );
			myEntity.getEmbedded().setOneToOneOwningSide( null );
		}, "embedded" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14798")
	public void oneToOne_nonOwningSide() {
		// null to non-null
		testUpdate( (s, myEntity) -> {
			MyOtherEntity other = s.get( MyOtherEntity.class, 1L );
			myEntity.setOneToOneNonOwningSide( other );
			other.setOneToOneOwningSide( myEntity );
		}, "oneToOneNonOwningSide" );

		// non-null to non-null
		testUpdate( (s, myEntity) -> {
			myEntity.getOneToOneNonOwningSide().setOneToOneOwningSide( null );
			MyOtherEntity other = s.get( MyOtherEntity.class, 2L );
			myEntity.setOneToOneNonOwningSide( other );
			other.setOneToOneOwningSide( myEntity );
		}, "oneToOneNonOwningSide" );

		// non-null to null
		testUpdate( (s, myEntity) -> {
			myEntity.getOneToOneNonOwningSide().setOneToOneOwningSide( null );
			myEntity.setOneToOneNonOwningSide( null );
		}, "oneToOneNonOwningSide" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14798")
	public void embedded_oneToOne_nonOwningSide() {
		// null to non-null
		testUpdate( (s, myEntity) -> {
			MyOtherEntity other = s.get( MyOtherEntity.class, 1L );
			myEntity.getEmbedded().setOneToOneNonOwningSide( other );
			other.setEmbeddedOneToOneOwningSide( myEntity );
		}, "embedded" );

		// non-null to non-null
		testUpdate( (s, myEntity) -> {
			myEntity.getEmbedded().getOneToOneNonOwningSide().setEmbeddedOneToOneOwningSide( null );
			MyOtherEntity other = s.get( MyOtherEntity.class, 2L );
			myEntity.getEmbedded().setOneToOneNonOwningSide( other );
			other.setEmbeddedOneToOneOwningSide( myEntity );
		}, "embedded" );

		// non-null to null
		testUpdate( (s, myEntity) -> {
			myEntity.getEmbedded().getOneToOneNonOwningSide().setEmbeddedOneToOneOwningSide( null );
			myEntity.getEmbedded().setOneToOneNonOwningSide( null );
		}, "embedded" );
	}

	@Test
	public void manyToOne() {
		// null to non-null
		testUpdate( (s, myEntity) -> {
			MyOtherEntity other = s.get( MyOtherEntity.class, 1L );
			myEntity.setManyToOne( other );
			other.getOneToMany().add( myEntity );
		}, "manyToOne" );

		// non-null to non-null
		testUpdate( (s, myEntity) -> {
			myEntity.getManyToOne().getOneToMany().clear();
			MyOtherEntity other = s.get( MyOtherEntity.class, 2L );
			myEntity.setManyToOne( other );
			other.getOneToMany().add( myEntity );
		}, "manyToOne" );

		// non-null to null
		testUpdate( (s, myEntity) -> {
			myEntity.getManyToOne().getOneToMany().clear();
			myEntity.setManyToOne( null );
		}, "manyToOne" );
	}

	@Test
	public void embedded_manyToOne() {
		// null to non-null
		testUpdate( (s, myEntity) -> {
			MyOtherEntity other = s.get( MyOtherEntity.class, 1L );
			myEntity.getEmbedded().setManyToOne( other );
			other.getEmbeddedOneToMany().add( myEntity );
		}, "embedded" );

		// non-null to non-null
		testUpdate( (s, myEntity) -> {
			myEntity.getEmbedded().getManyToOne().getEmbeddedOneToMany().clear();
			MyOtherEntity other = s.get( MyOtherEntity.class, 2L );
			myEntity.getEmbedded().setManyToOne( other );
			other.getEmbeddedOneToMany().add( myEntity );
		}, "embedded" );

		// non-null to null
		testUpdate( (s, myEntity) -> {
			myEntity.getEmbedded().getManyToOne().getEmbeddedOneToMany().clear();
			myEntity.getEmbedded().setManyToOne( null );
		}, "embedded" );
	}

	private void testUpdate(Consumer<MyEntity> update, String... updatedProperties) {
		testUpdate( (s, myEntity) -> update.accept( myEntity ), updatedProperties );
	}

	private void testUpdate(BiConsumer<Session, MyEntity> update, String... updatedProperties) {
		inTransaction( s -> {
			MyEntity myEntity = s.get( MyEntity.class, 1L );
			update.accept( s, myEntity );
		} );
		ArgumentCaptor<PostUpdateEvent> eventCaptor = ArgumentCaptor.forClass( PostUpdateEvent.class );
		verify( listenerMock, atLeastOnce() ).onPostUpdate( eventCaptor.capture() );
		clearInvocations( listenerMock );

		// Ignore events on MyOtherEntity
		List<PostUpdateEvent> events = eventCaptor.getAllValues().stream()
				.filter( e -> MyEntity.class.equals( e.getPersister().getMappedClass() ) )
				.collect( Collectors.toList() );
		assertThat( events ).hasSize( 1 );
		PostUpdateEvent event = events.get( 0 );

		Set<String> dirtyPropertyNames = Arrays.stream( event.getDirtyProperties() )
				.mapToObj( i -> event.getPersister().getPropertyNames()[i] )
				.collect( Collectors.toSet() );
		assertThat( dirtyPropertyNames )
				.containsExactlyInAnyOrder( updatedProperties );
	}

	@MappedSuperclass
	public static class MyMappedSuperclass {
		@Basic
		private String basic;

		@OneToOne
		private MyOtherEntity oneToOneOwningSide;

		@ManyToOne
		private MyOtherEntity manyToOne;

		public String getBasic() {
			return basic;
		}

		public void setBasic(String basic) {
			this.basic = basic;
		}

		public MyOtherEntity getOneToOneOwningSide() {
			return oneToOneOwningSide;
		}

		public void setOneToOneOwningSide(MyOtherEntity oneToOneOwningSide) {
			this.oneToOneOwningSide = oneToOneOwningSide;
		}

		public MyOtherEntity getManyToOne() {
			return manyToOne;
		}

		public void setManyToOne(MyOtherEntity manyToOne) {
			this.manyToOne = manyToOne;
		}
	}

	@Embeddable
	public static class MyEmbeddable extends MyMappedSuperclass {

		@OneToOne(mappedBy = "embeddedOneToOneOwningSide")
		private MyOtherEntity oneToOneNonOwningSide;

		public MyOtherEntity getOneToOneNonOwningSide() {
			return oneToOneNonOwningSide;
		}

		public void setOneToOneNonOwningSide(MyOtherEntity oneToOneNonOwningSide) {
			this.oneToOneNonOwningSide = oneToOneNonOwningSide;
		}
	}

	@Entity(name = "MyEntity")
	public static class MyEntity extends MyMappedSuperclass {
		@Id
		private Long id;

		@OneToOne(mappedBy = "oneToOneOwningSide")
		private MyOtherEntity oneToOneNonOwningSide;

		@Embedded
		private MyEmbeddable embedded;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MyOtherEntity getOneToOneNonOwningSide() {
			return oneToOneNonOwningSide;
		}

		public void setOneToOneNonOwningSide(MyOtherEntity oneToOneNonOwningSide) {
			this.oneToOneNonOwningSide = oneToOneNonOwningSide;
		}

		public MyEmbeddable getEmbedded() {
			return embedded;
		}

		public void setEmbedded(MyEmbeddable embedded) {
			this.embedded = embedded;
		}
	}

	@Entity(name = "MyOtherEntity")
	public static class MyOtherEntity {
		@Id
		private Long id;

		@OneToOne
		private MyEntity oneToOneOwningSide;

		@OneToOne(mappedBy = "oneToOneOwningSide")
		private MyEntity oneToOneNonOwningSide;

		@OneToMany(mappedBy = "manyToOne")
		private List<MyEntity> oneToMany;

		@OneToOne
		private MyEntity embeddedOneToOneOwningSide;

		@OneToOne(mappedBy = "embedded.oneToOneOwningSide")
		private MyEntity embeddedOneToOneNonOwningSide;

		@OneToMany(mappedBy = "embedded.manyToOne")
		private List<MyEntity> embeddedOneToMany;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MyEntity getOneToOneOwningSide() {
			return oneToOneOwningSide;
		}

		public void setOneToOneOwningSide(MyEntity oneToOneOwningSide) {
			this.oneToOneOwningSide = oneToOneOwningSide;
		}

		public MyEntity getOneToOneNonOwningSide() {
			return oneToOneNonOwningSide;
		}

		public void setOneToOneNonOwningSide(MyEntity oneToOneNonOwningSide) {
			this.oneToOneNonOwningSide = oneToOneNonOwningSide;
		}

		public List<MyEntity> getOneToMany() {
			return oneToMany;
		}

		public void setOneToMany(List<MyEntity> oneToMany) {
			this.oneToMany = oneToMany;
		}

		public MyEntity getEmbeddedOneToOneOwningSide() {
			return embeddedOneToOneOwningSide;
		}

		public void setEmbeddedOneToOneOwningSide(MyEntity embeddedOneToOneOwningSide) {
			this.embeddedOneToOneOwningSide = embeddedOneToOneOwningSide;
		}

		public MyEntity getEmbeddedOneToOneNonOwningSide() {
			return embeddedOneToOneNonOwningSide;
		}

		public void setEmbeddedOneToOneNonOwningSide(MyEntity embeddedOneToOneNonOwningSide) {
			this.embeddedOneToOneNonOwningSide = embeddedOneToOneNonOwningSide;
		}

		public List<MyEntity> getEmbeddedOneToMany() {
			return embeddedOneToMany;
		}

		public void setEmbeddedOneToMany(List<MyEntity> embeddedOneToMany) {
			this.embeddedOneToMany = embeddedOneToMany;
		}
	}
}
