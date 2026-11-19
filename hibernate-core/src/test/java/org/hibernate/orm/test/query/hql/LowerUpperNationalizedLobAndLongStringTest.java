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

@DomainModel(annotatedClasses = LowerUpperNationalizedLobAndLongStringTest.NationalizedText.class)
@SessionFactory
@JiraKey("HHH-18954")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNClob.class)
class LowerUpperNationalizedLobAndLongStringTest {
	private static final String TEXT = "Abc Àà Ωω ".repeat( 10000 ) + "End";

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new NationalizedText( 1, TEXT, TEXT ) );
			session.persist( new NationalizedText( 2, "No match", "No match" ) );
			session.persist( new NationalizedText( 3, null, null ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsLong32CaseConversion.class)
	void lowerLong32Nvarchar(SessionFactoryScope scope) {
		testCaseConversion( scope, "longText", true );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsLong32CaseConversion.class)
	void upperLong32Nvarchar(SessionFactoryScope scope) {
		testCaseConversion( scope, "longText", false );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsClobCaseConversion.class)
	void lowerNclob(SessionFactoryScope scope) {
		testCaseConversion( scope, "lobText", true );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsClobCaseConversion.class)
	void upperNclob(SessionFactoryScope scope) {
		testCaseConversion( scope, "lobText", false );
	}

	private void testCaseConversion(SessionFactoryScope scope, String attribute, boolean lower) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select e." + attribute + " from NationalizedText e where e.id = 1", String.class
			).getSingleResult() ).isEqualTo( TEXT );
			final String function = lower ? "lower" : "upper";
			final String expected = lower ? TEXT.toLowerCase( Locale.ROOT ) : TEXT.toUpperCase( Locale.ROOT );
			assertThat( session.createQuery(
					"select " + function + "(e." + attribute + ") from NationalizedText e where e.id = 1",
					String.class
			).getSingleResult() ).isEqualTo( expected );
			assertThat( session.createQuery(
					"select " + function + "(e." + attribute + ") from NationalizedText e where e.id = 3",
					String.class
			).getSingleResult() ).isNull();

			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( NationalizedText.class );
			final var root = query.from( NationalizedText.class );
			query.where( builder.like(
					lower ? builder.lower( root.get( attribute ) ) : builder.upper( root.get( attribute ) ),
					builder.parameter( String.class, "pattern" )
			) );
			final var results = session.createQuery( query )
					.setParameter( "pattern", lower ? "%abc àà ωω%" : "%ABC ÀÀ ΩΩ%" )
					.getResultList();
			assertThat( results ).extracting( entity -> entity.id ).containsExactly( 1 );
		} );
	}

	@Entity(name = "NationalizedText")
	static class NationalizedText {
		@Id
		private Integer id;

		@JdbcTypeCode(SqlTypes.LONG32NVARCHAR)
		@Column(name = "long_text", length = 1_000_000)
		private String longText;

		@JdbcTypeCode(SqlTypes.NCLOB)
		private String lobText;

		NationalizedText() {
		}

		NationalizedText(int id, String longText, String lobText) {
			this.id = id;
			this.longText = longText;
			this.lobText = lobText;
		}
	}
}
