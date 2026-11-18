package org.hibernate.orm.test.jpa.callbacks.jpa4;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.models.ModelsException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/jpa/callbacks/jpa4/entity-listener-multiple-callbacks.orm.xml"
)
@SessionFactory
public class EntityListenerXmlMultipleCallbackTests {

	@BeforeEach
	void reset() {
		Events.reset();
	}

	@Test
	void entityListenerCanDeclareSameEventElementMultipleTimes(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new MultiElementEntity( 1, "multi" ) ) );

		assertThat( Events.names ).containsExactlyInAnyOrder(
				"listener-object:MultiElementEntity",
				"listener-typed:multi"
		);
	}

	@Test
	void entityListenerRegistersAllOverloadsMatchingMethodName(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new OverloadedEntity( 1, "overloaded" ) ) );

		assertThat( Events.names ).containsExactlyInAnyOrder(
				"overloaded-object:OverloadedEntity",
				"overloaded-typed:overloaded"
		);
	}

	@Test
	void entityListenerRejectsMultipleCallbacksForSameEventAndParameterType(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> new MetadataSources( registryScope.getRegistry() )
				.addResource( "org/hibernate/orm/test/jpa/callbacks/jpa4/entity-listener-duplicate-callbacks.orm.xml" )
				.buildMetadata() )
				.isInstanceOf( ModelsException.class )
				.hasMessageContaining( DuplicateListener.class.getSimpleName() );
	}

	public static class Events {
		static final List<String> names = new ArrayList<>();

		static void reset() {
			names.clear();
		}
	}

	public static class MultiElementEntity {
		private Integer id;
		private String name;

		public MultiElementEntity() {
		}

		public MultiElementEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class MultiElementListener {
		void anyPersisted(Object entity) {
			Events.names.add( "listener-object:" + entity.getClass().getSimpleName() );
		}

		void typedPersisted(MultiElementEntity entity) {
			Events.names.add( "listener-typed:" + entity.name );
		}
	}

	public static class OverloadedEntity {
		private Integer id;
		private String name;

		public OverloadedEntity() {
		}

		public OverloadedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class OverloadedListener {
		void onPersist(Object entity) {
			Events.names.add( "overloaded-object:" + entity.getClass().getSimpleName() );
		}

		void onPersist(OverloadedEntity entity) {
			Events.names.add( "overloaded-typed:" + entity.name );
		}
	}

	public static class DuplicateEntity {
		private Integer id;
		private String name;
	}

	public static class DuplicateListener {
		void first(Object entity) {
			Events.names.add( "first" );
		}

		void second(Object entity) {
			Events.names.add( "second" );
		}
	}
}
