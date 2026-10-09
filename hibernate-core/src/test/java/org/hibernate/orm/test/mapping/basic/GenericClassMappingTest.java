package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Mapping a Class whose type argument is an unresolved entity type variable.
///
/// @author Steve Ebersole
@JiraKey("HHH-17974")
@DomainModel(annotatedClasses = GenericClassMappingTest.GenericEntity.class)
@SessionFactory
public class GenericClassMappingTest {

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testImplicitMapping(SessionFactoryScope scope) {
		final var entity = scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( GenericEntity.class );
		final var attribute = (BasicAttributeMapping) entity.findAttributeMapping( "clazz" );
		assertSame( Class.class, attribute.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() );
		assertEquals( Types.VARCHAR, attribute.getJdbcMapping().getJdbcType().getJdbcTypeCode() );
	}

	@Test
	void testRoundTripAndQueryParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new GenericEntity<>( 1L, String.class ) );
			session.persist( new GenericEntity<>( 2L, Integer.class ) );
		} );
		scope.inTransaction( session -> {
			assertSame( String.class, session.find( GenericEntity.class, 1L ).clazz );
			final var result = session.createQuery(
					"from GenericClassEntity where clazz = :clazz", GenericEntity.class )
					.setParameter( "clazz", Integer.class )
					.getSingleResult();
			assertEquals( 2L, result.id );
			assertSame( Integer.class, result.clazz );
		} );
	}

	@Entity(name = "GenericClassEntity")
	public static class GenericEntity<T> {
		@Id
		private Long id;

		@Column(name = "ACCOUNT_TYPE")
		private Class<T> clazz;

		public GenericEntity() {
		}

		GenericEntity(Long id, Class<T> clazz) {
			this.id = id;
			this.clazz = clazz;
		}
	}
}
