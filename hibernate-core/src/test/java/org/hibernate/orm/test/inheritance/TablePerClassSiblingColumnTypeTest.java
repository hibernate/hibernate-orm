package org.hibernate.orm.test.inheritance;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Exercises sibling attributes mapped to the same column name with incompatible types.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		TablePerClassSiblingColumnTypeTest.Base.class,
		TablePerClassSiblingColumnTypeTest.Numeric.class,
		TablePerClassSiblingColumnTypeTest.Textual.class
})
@SessionFactory
@JiraKey("HHH-20964")
@JiraKey("HHH-20965")
class TablePerClassSiblingColumnTypeTest {
	@BeforeEach
	void populate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var numeric = new Numeric();
			numeric.id = 1L;
			numeric.a = 11;
			numeric.b = 22;
			session.persist( numeric );

			var textual = new Textual();
			textual.id = 2L;
			textual.b = "not-a-number";
			textual.c = "text-only";
			session.persist( textual );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testConcreteSubtypeLoading(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertRows( List.of(
				session.get( Numeric.class, 1L ),
				session.get( Textual.class, 2L )
		) ) );
	}

	@Test
	// MySQL and MariaDB both actually allow this :/
	@SkipForDialect(dialectClass = MySQLDialect.class)
	@SkipForDialect(dialectClass = MariaDBDialect.class)
	@FailureExpected(jiraKey = "HHH-5605", reason = "The polymorphic union combines same-named sibling columns with incompatible types")
	void testPolymorphicQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertRows( session.createQuery(
				"from SiblingColumnBase b order by b.id", Base.class
		).getResultList() ) );
	}

	private void assertRows(List<? extends Base> rows) {
		assertThat( rows ).hasSize( 2 );
		assertThat( rows.get( 0 ) ).isInstanceOfSatisfying( Numeric.class, numeric -> {
			assertThat( numeric.id ).isEqualTo( 1L );
			assertThat( numeric.a ).isEqualTo( 11 );
			assertThat( numeric.b ).isEqualTo( 22 );
		} );
		assertThat( rows.get( 1 ) ).isInstanceOfSatisfying( Textual.class, textual -> {
			assertThat( textual.id ).isEqualTo( 2L );
			assertThat( textual.b ).isEqualTo( "not-a-number" );
			assertThat( textual.c ).isEqualTo( "text-only" );
		} );
	}

	@Entity(name = "SiblingColumnBase")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class Base {
		@Id
		Long id;
	}

	@Entity(name = "SiblingColumnNumeric")
	@Table(name = "sibling_column_numeric")
	public static class Numeric extends Base {
		@Column(name = "a")
		Integer a;

		@Column(name = "b")
		Integer b;
	}

	@Entity(name = "SiblingColumnTextual")
	@Table(name = "sibling_column_textual")
	public static class Textual extends Base {
		@Column(name = "b")
		String b;

		@Column(name = "c")
		String c;
	}
}
