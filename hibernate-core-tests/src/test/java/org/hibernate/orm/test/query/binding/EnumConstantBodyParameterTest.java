package org.hibernate.orm.test.query.binding;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-21070")
@DomainModel(annotatedClasses = EnumConstantBodyParameterTest.Item.class)
@SessionFactory
public class EnumConstantBodyParameterTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Item( 1L, MappedColor.GREEN ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testHqlQueryParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long result = session.createQuery( "select count(i) from Item i where :value is not null", Long.class )
					.setParameter( "value", ColorWithClassBody.GREEN )
					.getSingleResult();
			assertThat( result ).isEqualTo( 1L );
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testNativeQueryParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Integer result = session.createNativeQuery( "select cast(:value as integer)", Integer.class )
					.setParameter( "value", ColorWithClassBody.GREEN )
					.getSingleResult();
			assertThat( result ).isEqualTo( 1 );
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testNativeQueryOrdinaryEnumParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Integer result = session.createNativeQuery( "select cast(:value as integer)", Integer.class )
					.setParameter( "value", Color.GREEN )
					.getSingleResult();
			assertThat( result ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testHqlQueryOrdinaryEnumParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long result = session.createQuery( "select count(i) from Item i where :value is not null", Long.class )
					.setParameter( "value", Color.GREEN )
					.getSingleResult();
			assertThat( result ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testMappedEnumParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long result = session.createQuery( "select i.id from Item i where i.color = :value", Long.class )
					.setParameter( "value", MappedColor.GREEN )
					.getSingleResult();
			assertThat( result ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testMappedNullParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat(
				session.createQuery( "select i.id from Item i where i.color = :value", Long.class )
						.setParameter( "value", null )
						.getResultList()
		).isEmpty() );
	}

	public enum Color {
		RED,
		GREEN
	}

	public enum MappedColor {
		RED,
		GREEN {
			@Override
			public String toString() {
				return "green";
			}
		}
	}

	public enum ColorWithClassBody {
		RED,
		GREEN {
			@Override
			public String toString() {
				return "green";
			}
		}
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		private Long id;

		@Enumerated(EnumType.STRING)
		private MappedColor color;

		public Item() {
		}

		public Item(Long id, MappedColor color) {
			this.id = id;
			this.color = color;
		}
	}
}
