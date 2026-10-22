package org.hibernate.orm.test.mapping.enumeratedvalue;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JiraKey("HHH-20410")
@ServiceRegistry
public class ImplicitEnumeratedValueValidationTests {
	@Test
	void testExplicitOrdinal(ServiceRegistryScope scope) {
		assertInvalidOrdinal( ExplicitOrdinalEntity.class, scope );
	}

	@Test
	void testDefaultEnumerated(ServiceRegistryScope scope) {
		assertInvalidOrdinal( DefaultEnumeratedEntity.class, scope );
	}

	@Test
	void testNonFinalString(ServiceRegistryScope scope) {
		assertInvalidOrdinal( NonFinalEntity.class, scope );
	}

	@Test
	void testCharDoesNotImplyString(ServiceRegistryScope scope) {
		assertInvalidOrdinal( CharEntity.class, scope );
	}

	private void assertInvalidOrdinal(Class<?> entityClass, ServiceRegistryScope scope) {
		assertThatThrownBy( () -> new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( entityClass ).buildMetadata() )
				.isInstanceOf( MappingException.class )
				.hasMessageStartingWith( "@EnumeratedValue for EnumType.ORDINAL" );
	}

	@Entity
	public static class ExplicitOrdinalEntity {
		@Id
		private Integer id;
		@Enumerated(EnumType.ORDINAL)
		private EnumeratedValueTests.Gender gender;
	}

	@Entity
	public static class DefaultEnumeratedEntity {
		@Id
		private Integer id;
		@Enumerated
		private EnumeratedValueTests.Gender gender;
	}

	@Entity
	public static class NonFinalEntity {
		@Id
		private Integer id;
		private NonFinalCode value;
	}

	@Entity
	public static class CharEntity {
		@Id
		private Integer id;
		private CharCode value;
	}

	enum NonFinalCode {
		VALUE( "V" );

		@EnumeratedValue
		private String code;

		NonFinalCode(String code) {
			this.code = code;
		}
	}

	enum CharCode {
		VALUE( 'V' );

		@EnumeratedValue
		private final char code;

		CharCode(char code) {
			this.code = code;
		}
	}
}
