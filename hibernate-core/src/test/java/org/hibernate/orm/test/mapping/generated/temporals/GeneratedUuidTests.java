/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated.temporals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test illustrating usage of {@link ValueGenerationType}
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = GeneratedUuidTests.GeneratedUuidEntity.class )
@SessionFactory
public class GeneratedUuidTests {
	@Test
	public void test(SessionFactoryScope scope) {
		final GeneratedUuidEntity created = scope.fromTransaction( (session) -> {
			final GeneratedUuidEntity entity = new GeneratedUuidEntity( 1, "tsifr" );
			session.persist( entity );
			return entity;
		} );

		assertThat( created.createdUuid ).isNotNull();
		assertThat( created.updatedUuid ).isNotNull();

		created.name = "first";

		// then changing
		final GeneratedUuidEntity merged = scope.fromTransaction( (session) -> {
			return session.merge( created );
		} );

		assertThat( merged ).isNotNull();
		assertThat( merged.createdUuid ).isNotNull();
		assertThat( merged.updatedUuid ).isNotNull();
		assertThat( merged.createdUuid ).isEqualTo( created.createdUuid );
		assertThat( merged.updatedUuid ).isNotEqualTo( created.updatedUuid );

		assertThat( merged ).isNotNull();

		// lastly, make sure we can load it..
		final GeneratedUuidEntity loaded = scope.fromTransaction( (session) -> {
			return session.get( GeneratedUuidEntity.class, 1 );
		} );

		assertThat( loaded ).isNotNull();

		assertThat( loaded.createdUuid ).isEqualTo( merged.createdUuid );
		assertThat( loaded.updatedUuid ).isEqualTo( merged.updatedUuid );
	}

	//tag::mapping-generated-custom-ex2[]
	@ValueGenerationType( generatedBy = UuidValueGeneration.class )
	@Retention(RetentionPolicy.RUNTIME)
	@Target( { ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE } )
	@Inherited
	public @interface GeneratedUuidValue {
		GenerationTiming timing();
	}
	//end::mapping-generated-custom-ex2[]

	//tag::mapping-generated-custom-ex3[]
	public static class UuidValueGeneration implements AnnotationValueGeneration<GeneratedUuidValue>, ValueGenerator<UUID> {
		private GenerationTiming timing;

		@Override
		public void initialize(GeneratedUuidValue annotation, Class<?> propertyType) {
			timing = annotation.timing();
		}

		@Override
		public GenerationTiming getGenerationTiming() {
			return timing;
		}

		@Override
		public ValueGenerator<?> getValueGenerator() {
			return this;
		}

		@Override
		public boolean referenceColumnInSql() {
			return false;
		}

		@Override
		public String getDatabaseGeneratedReferencedColumnValue() {
			return null;
		}

		@Override
		public UUID generateValue(Session session, Object owner) {
			return UUID.randomUUID();
		}
	}
	//end::mapping-generated-custom-ex3[]

	@Entity( name = "GeneratedUuidEntity" )
	@Table( name = "t_gen_uuid" )
	public static class GeneratedUuidEntity {
	    @Id
	    public Integer id;
	    @Basic
	    public String name;

		//tag::mapping-generated-custom-ex1[]
		@GeneratedUuidValue( timing = GenerationTiming.INSERT )
		public UUID createdUuid;

		@GeneratedUuidValue( timing = GenerationTiming.ALWAYS )
	    public UUID updatedUuid;
		//end::mapping-generated-custom-ex1[]

		public GeneratedUuidEntity() {
		}

		public GeneratedUuidEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
