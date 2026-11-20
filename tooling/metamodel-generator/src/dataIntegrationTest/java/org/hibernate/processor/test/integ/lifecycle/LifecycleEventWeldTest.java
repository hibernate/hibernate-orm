package org.hibernate.processor.test.integ.lifecycle;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jakarta.data.event.LifecycleEvent;
import jakarta.data.event.PostUpsertEvent;
import jakarta.data.event.PreInsertEvent;
import jakarta.data.event.PreUpsertEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.EventMetadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.jboss.weld.environment.se.Weld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LifecycleEventWeldTest {
	@Test
	void generatedListenerDeliversConcreteAndGenericEvents() {
		try ( var container = new Weld().disableDiscovery()
				.addBeanClasses( Observers.class, _LifecycleItem.class ).initialize() ) {
			final var registry = new StandardServiceRegistryBuilder()
					.applySetting( "jakarta.persistence.jdbc.url", "jdbc:h2:mem:processor_lifecycle;DB_CLOSE_DELAY=-1" )
					.applySetting( "jakarta.persistence.jdbc.driver", "org.h2.Driver" )
					.applySetting( "hibernate.hbm2ddl.auto", "create-drop" )
					.applySetting( "jakarta.persistence.bean.manager", container.getBeanManager() )
					.build();
			try ( var factory = new MetadataSources( registry )
					.addAnnotatedClass( LifecycleItem.class )
					.addAnnotatedClass( _LifecycleItem.class )
					.buildMetadata().buildSessionFactory() ) {
				final var item = new LifecycleItem();
				item.id = 1L;
				item.name = "before";
				try ( var session = factory.openSession() ) {
					final var tx = session.beginTransaction();
					session.persist( item );
					tx.commit();
				}
				try ( var session = factory.openStatelessSession() ) {
					final var tx = session.beginTransaction();
					item.name = "after";
					session.upsert( item );
					tx.commit();
				}
				final var observer = container.select( Observers.class ).get();
				assertEquals( 1, observer.count( "preInsert" ) );
				assertEquals( 1, observer.count( "preUpsert" ) );
				assertEquals( 1, observer.count( "postUpsert" ) );
				for ( var event : List.of( "PreInsertEvent", "PostInsertEvent", "PreUpsertEvent", "PostUpsertEvent" ) ) {
					assertEquals( 1, observer.count( "item:" + event ) );
					assertEquals( 1, observer.count( "any:" + event ) );
				}
				assertEquals( 0, observer.wrongCount() );
				final var eventType = assertInstanceOf( ParameterizedType.class, observer.eventType() );
				assertEquals( PreInsertEvent.class, eventType.getRawType() );
				assertArrayEquals( new Type[] { LifecycleItem.class }, eventType.getActualTypeArguments() );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( registry );
			}
		}
	}

	@ApplicationScoped
	public static class Observers {
		private final List<String> events = new ArrayList<>();
		private Type eventType;
		private int wrong;

		void preInsert(@Observes PreInsertEvent<LifecycleItem> event, EventMetadata metadata) {
			events.add( "preInsert" );
			eventType = metadata.getType();
		}

		void preUpsert(@Observes PreUpsertEvent<LifecycleItem> event) {
			events.add( "preUpsert" );
		}

		void postUpsert(@Observes PostUpsertEvent<LifecycleItem> event) {
			events.add( "postUpsert" );
		}

		void item(@Observes LifecycleEvent<LifecycleItem> event) {
			events.add( "item:" + event.getClass().getSimpleName() );
		}

		void any(@Observes LifecycleEvent<?> event) {
			events.add( "any:" + event.getClass().getSimpleName() );
		}

		void other(@Observes PreUpsertEvent<Other> event) {
			wrong++;
		}

		public long count(String value) {
			return events.stream().filter( value::equals ).count();
		}

		public int wrongCount() {
			return wrong;
		}

		public Type eventType() {
			return eventType;
		}
	}

	public static class Other {
	}
}
