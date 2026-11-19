package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = LowerUpperLobAndLongStringTest.Text.class)
@SessionFactory
@JiraKey("HHH-18954")
class LowerUpperLobAndLongStringTest {
	private static final String LONG_TEXT = "Aaaa aaaa ".repeat( 10000 ) + "End";
	private static final String CLOB_TEXT = "Aaaa Àà ".repeat( 10000 ) + "End";

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Text( 1L, LONG_TEXT, CLOB_TEXT ) );
			session.persist( new Text( 2L, "No match", "No match" ) );
			session.persist( new Text( 3L, null, null ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsLong32CaseConversion.class)
	void lowerLong32Varchar(SessionFactoryScope scope) {
		testCaseConversion( scope, "longText", true );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsLong32CaseConversion.class)
	void upperLong32Varchar(SessionFactoryScope scope) {
		testCaseConversion( scope, "longText", false );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsClobCaseConversion.class)
	void lowerClob(SessionFactoryScope scope) {
		testCaseConversion( scope, "clobText", true );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsClobCaseConversion.class)
	void upperClob(SessionFactoryScope scope) {
		testCaseConversion( scope, "clobText", false );
	}

	private void testCaseConversion(SessionFactoryScope scope, String attribute, boolean lower) {
		final String sample = attribute.equals( "longText" ) ? "Aaaa aaaa " : "Aaaa Àà ";
		final String text = attribute.equals( "longText" ) ? LONG_TEXT : CLOB_TEXT;
		final String pattern = "%" + (lower ? sample.toLowerCase( Locale.ROOT ) : sample.toUpperCase( Locale.ROOT ))
				.stripTrailing() + "%";

		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select e." + attribute + " from Text e where e.id = 1", String.class
			).getSingleResult() ).isEqualTo( text );
			final String function = lower ? "lower" : "upper";
			final String expected = lower ? text.toLowerCase( Locale.ROOT ) : text.toUpperCase( Locale.ROOT );
			assertThat( session.createQuery(
					"select " + function + "(e." + attribute + ") from Text e where e.id = 1",
					String.class
			).getSingleResult() ).isEqualTo( expected );
			assertThat( session.createQuery(
					"select " + function + "(e." + attribute + ") from Text e where e.id = 3",
					String.class
			).getSingleResult() ).isNull();

			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Text.class );
			final var root = query.from( Text.class );
			query.where( builder.like(
					lower ? builder.lower( root.get( attribute ) ) : builder.upper( root.get( attribute ) ),
					builder.parameter( String.class, "pattern" )
			) );

			final var result = session.createQuery( query )
					.setParameter( "pattern", pattern )
					.getResultList();
			assertThat( result ).hasSize( 1 );
			final var entity = result.get( 0 );
			assertThat( entity.id ).isEqualTo( 1L );
			assertThat( attribute.equals( "longText" ) ? entity.longText : entity.clobText ).isEqualTo( text );
		} );
	}

	@Entity(name = "Text")
	static class Text {
		@Id
		private Long id;

		@JdbcTypeCode(SqlTypes.LONG32VARCHAR)
		@Column(name = "long_text", length = 1_000_000)
		private String longText;

		@JdbcTypeCode(SqlTypes.CLOB)
		private String clobText;

		Text() {
		}

		Text(long id, String longText, String clobText) {
			this.id = id;
			this.longText = longText;
			this.clobText = clobText;
		}
	}
}
