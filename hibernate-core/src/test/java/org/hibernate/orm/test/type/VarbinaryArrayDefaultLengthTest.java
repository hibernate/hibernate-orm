package org.hibernate.orm.test.type;

import org.hibernate.cfg.MappingSettings;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
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
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
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
