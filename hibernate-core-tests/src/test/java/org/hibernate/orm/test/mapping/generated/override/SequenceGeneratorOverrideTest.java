package org.hibernate.orm.test.mapping.generated.override;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// @author Steve Ebersole
@DomainModel(
		annotatedClasses = {
				SequenceGeneratorOverrideTest.Entity1.class,
				SequenceGeneratorOverrideTest.Entity2.class,
				SequenceGeneratorOverrideTest.SharedEntity1.class,
				SequenceGeneratorOverrideTest.SharedEntity2.class,
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-4309")
public class SequenceGeneratorOverrideTest {

	@Test
	public void test(SessionFactoryScope scope) {
		SessionFactoryImplementor sf = scope.getSessionFactory();
		EntityPersister p1 = sf.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Entity1.class.getName() );
		EntityPersister p2 = sf.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Entity2.class.getName() );

		SequenceStyleGenerator generator1 = (SequenceStyleGenerator) p1.getGenerator();
		SequenceStyleGenerator generator2 = (SequenceStyleGenerator) p2.getGenerator();
		assertThat( generator1.getDatabaseStructure().getPhysicalName().render() )
				.isEqualTo( "base_sequence" );
		assertThat( generator2.getDatabaseStructure().getPhysicalName().render() )
				.isEqualTo( "sub_sequence" );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-15103")
	public void testMappedSuperclassSharedSequence(SessionFactoryScope scope) {
		final var first = new SharedEntity1();
		final var second = new SharedEntity2();
		final var mappingMetamodel = scope.getSessionFactory().getMappingMetamodel();
		for ( var entityClass : new Class<?>[] { SharedEntity1.class, SharedEntity2.class } ) {
			final var generator = (SequenceStyleGenerator) mappingMetamodel
					.getEntityDescriptor( entityClass ).getGenerator();
			assertThat( generator.getDatabaseStructure().getPhysicalName().render() )
					.isEqualTo( "shared_sequence" );
		}

		scope.inTransaction( session -> {
			session.persist( first );
			session.persist( second );
			session.flush();
			assertThat( first.id ).isNotNull();
			assertThat( second.id ).isEqualTo( first.id + 1 );
		} );

		scope.inTransaction( session -> {
			final var loadedFirst = session.find( SharedEntity1.class, first.id );
			final var loadedSecond = session.find( SharedEntity2.class, second.id );
			assertThat( loadedFirst ).isNotNull();
			assertThat( loadedSecond ).isNotNull();
			session.remove( loadedFirst );
			session.remove( loadedSecond );
		} );
	}

	@MappedSuperclass
	@SequenceGenerator(name = "SEQ_SHARED", sequenceName = "shared_sequence", allocationSize = 1)
	public static abstract class SharedBaseEntity {
		@Id
		@GeneratedValue(generator = "SEQ_SHARED")
		protected Long id;
	}

	@jakarta.persistence.Entity(name = "SharedEntity1")
	public static class SharedEntity1 extends SharedBaseEntity {
	}

	@jakarta.persistence.Entity(name = "SharedEntity2")
	public static class SharedEntity2 extends SharedBaseEntity {
	}

	//tag::identifiers-generators-sequence-override-example[]
	@MappedSuperclass
	@SequenceGenerator(name = "my-generator", sequenceName = "base_sequence")
	public static abstract class BaseEntity {
		@Id
		@GeneratedValue(generator = "my-generator")
		private Long id;
	//end::identifiers-generators-sequence-override-example[]

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}
	//tag::identifiers-generators-sequence-override-example[]
	}

	@jakarta.persistence.Entity(name = "Entity1")
	public static class Entity1 extends BaseEntity {
	}

	@jakarta.persistence.Entity(name = "Entity2")
	@SequenceGenerator(name = "my-generator", sequenceName = "sub_sequence")
	public static class Entity2 extends BaseEntity {
	}
	//end::identifiers-generators-sequence-override-example[]

}
