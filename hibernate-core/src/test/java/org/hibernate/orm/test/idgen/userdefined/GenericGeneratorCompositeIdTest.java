package org.hibernate.orm.test.idgen.userdefined;

import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test a generator declared with {@link GenericGenerator} on an attribute of a composite id.
 * <p>
 * {@link org.hibernate.id.GenericGeneratorGeneration} implements both {@link BeforeExecutionGenerator}
 * and {@link org.hibernate.generator.OnExecutionGenerator}, but the generator in this test only
 * generates the value before execution.
 */
@DomainModel(annotatedClasses = {
		GenericGeneratorCompositeIdTest.IdClassEntity.class,
		GenericGeneratorCompositeIdTest.EmbeddedIdEntity.class
})
@SessionFactory
public class GenericGeneratorCompositeIdTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@JiraKey( "HHH-21001" )
	void testIdClass(SessionFactoryScope scope) {
		final IdClassEntity entity = new IdClassEntity( 1L, "IdClass" );
		scope.inTransaction( session -> session.persist( entity ) );
		assertThat( entity.id ).isNotNull();

		scope.inTransaction( session -> {
			final IdClassEntity found = session.find( IdClassEntity.class, new IdClassPk( entity.version, entity.id ) );
			assertThat( found ).isNotNull();
			assertThat( found.name ).isEqualTo( entity.name );
		} );
	}

	@Test
	@JiraKey( "HHH-21001" )
	void testEmbeddedId(SessionFactoryScope scope) {
		final EmbeddedIdEntity entity = new EmbeddedIdEntity( new EmbeddedPk( 1L ), "EmbeddedId" );
		scope.inTransaction( session -> session.persist( entity ) );
		assertThat( entity.pk.id ).isNotNull();

		scope.inTransaction( session -> {
			final EmbeddedIdEntity found = session.find( EmbeddedIdEntity.class, entity.pk );
			assertThat( found ).isNotNull();
			assertThat( found.name ).isEqualTo( entity.name );
		} );
	}

	public static class CounterGenerator implements BeforeExecutionGenerator {
		private final AtomicLong counter = new AtomicLong();

		@Override
		public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
			return counter.incrementAndGet();
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EnumSet.of( EventType.INSERT );
		}
	}

	@Entity(name = "IdClassEntity")
	@IdClass(IdClassPk.class)
	public static class IdClassEntity {
		@Id
		Long version;

		@Id
		@GeneratedValue
		@GenericGenerator(type = CounterGenerator.class)
		Long id;

		String name;

		public IdClassEntity() {
		}

		public IdClassEntity(Long version, String name) {
			this.version = version;
			this.name = name;
		}
	}

	public record IdClassPk(Long version, Long id) {
	}

	@Entity(name = "EmbeddedIdEntity")
	public static class EmbeddedIdEntity {
		@EmbeddedId
		EmbeddedPk pk;

		String name;

		public EmbeddedIdEntity() {
		}

		public EmbeddedIdEntity(EmbeddedPk pk, String name) {
			this.pk = pk;
			this.name = name;
		}
	}

	@Embeddable
	public static class EmbeddedPk {
		Long version;

		@GeneratedValue
		@GenericGenerator(type = CounterGenerator.class)
		Long id;

		public EmbeddedPk() {
		}

		public EmbeddedPk(Long version) {
			this.version = version;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof EmbeddedPk that
				&& Objects.equals( version, that.version )
				&& Objects.equals( id, that.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( version, id );
		}
	}
}
