package org.hibernate.orm.test.type;

import org.hibernate.cfg.MappingSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@ServiceRegistry(settings = @Setting(name = MappingSettings.PREFERRED_ARRAY_JDBC_TYPE, value = "VARBINARY"))
@DomainModel(annotatedClasses = VarbinaryArrayDefaultLengthTest.EntityWithArray.class)
@SessionFactory
@JiraKey("HHH-21055")
public class VarbinaryArrayDefaultLengthTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testArrayFitsDefaultColumnLength(SessionFactoryScope scope) {
		final Integer[] numbers = { 1, 2, 3, 4, 5, 6, 7, 8 };
		scope.inTransaction( session -> {
			final EntityWithArray entity = new EntityWithArray();
			entity.id = 1L;
			entity.numbers = numbers;
			session.persist( entity );
		} );
		scope.inTransaction( session ->
				assertThat( session.find( EntityWithArray.class, 1L ).numbers ).isEqualTo( numbers )
		);
	}

	@Entity(name = "EntityWithArray")
	public static class EntityWithArray {
		@Id
		Long id;
		Integer[] numbers;
	}
}
