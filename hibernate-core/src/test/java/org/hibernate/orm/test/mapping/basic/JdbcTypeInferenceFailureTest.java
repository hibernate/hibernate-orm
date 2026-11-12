package org.hibernate.orm.test.mapping.basic;

import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.type.descriptor.java.spi.JdbcTypeRecommendationException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ServiceRegistry
@JiraKey("HHH-21136")
class JdbcTypeInferenceFailureTest {
	@Test
	void unsupportedBasicType(ServiceRegistryScope scope) {
		var sources = new MetadataSources( scope.getRegistry() ).addAnnotatedClass( Person.class );
		var failure = assertThrows( JdbcTypeRecommendationException.class, sources::buildMetadata );
		assertThat( failure )
				.hasMessageContaining( Optional.class.getName() )
				.hasMessageContaining( Person.class.getName() + ".nickname" );
	}

	@Test
	void missingAssociationAnnotation(ServiceRegistryScope scope) {
		var sources = new MetadataSources( scope.getRegistry() ).addAnnotatedClass( Employee.class );
		var failure = assertThrows( MappingException.class, sources::buildMetadata );
		assertThat( failure )
				.hasMessageContaining( Employee.class.getName() + ".manager" )
				.hasMessageContaining( "missing association mapping annotation" );
	}

	@Entity
	static class Person {
		@Id long id;
		Optional<String> nickname;
	}

	@Entity
	static class Employee {
		@Id long id;
		Employee manager;
	}
}
