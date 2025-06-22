/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.temporals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.UUID;

import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

/**
 * Test illustrating usage of {@link ValueGenerationType}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = GeneratedUuidTests.GeneratedUuidEntity.class )
@SessionFactory
@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Driver or DB omit trailing zero bytes of a varbinary, making this test fail intermittently")
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
		final GeneratedUuidEntity loaded = scope.fromTransaction( (session) -> session.get( GeneratedUuidEntity.class, 1 ));

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
		EventType[] timing();
	}
	//end::mapping-generated-custom-ex2[]

	//tag::mapping-generated-custom-ex3[]
	public static class UuidValueGeneration implements BeforeExecutionGenerator {
		private final EnumSet<EventType> eventTypes;

		public UuidValueGeneration(GeneratedUuidValue annotation) {
			eventTypes = EventTypeSets.fromArray( annotation.timing() );
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return eventTypes;
		}

		@Override
		public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
			return SafeRandomUUIDGenerator.safeRandomUUID();
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
		@GeneratedUuidValue( timing = INSERT )
		public UUID createdUuid;

		@GeneratedUuidValue( timing = {INSERT, UPDATE} )
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
