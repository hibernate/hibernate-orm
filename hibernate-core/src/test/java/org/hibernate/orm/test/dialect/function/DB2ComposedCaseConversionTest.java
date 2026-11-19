package org.hibernate.orm.test.dialect.function;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = DB2ComposedCaseConversionTest.Text.class)
@SessionFactory
@RequiresDialect(value = DB2Dialect.class, matchSubTypes = false)
@JiraKey("HHH-18954")
class DB2ComposedCaseConversionTest {
	private static final String TEXT = "Abc Àà Ωω & < > \" ' \t\n".repeat( 6000 ) + "End";

	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Text( 0, TEXT ) );
			session.persist( new Text( 1, "" ) );
			session.persist( new Text( 2, null ) );
			session.persist( new Text( 3, "Unselected" ) );
		} );
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("composedCaseConversions")
	void composedCaseConversion(
			String expression,
			String expectedText,
			String expectedNull,
			String expectedUnselected,
			String pattern,
			SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery(
					"select " + expression + " from DB2CaseConversionText t order by t.id", String.class
			).getResultList() )
					.as( "%s", expression )
					.containsExactly( expectedText, "", expectedNull, expectedUnselected );
			assertThat( session.createSelectionQuery(
					"select t.id from DB2CaseConversionText t where " + expression + " like :pattern", Integer.class
			).setParameter( "pattern", pattern ).getResultList() ).containsExactly( 0 );
		} );
	}

	static Stream<Arguments> composedCaseConversions() {
		final String lowerText = TEXT.toUpperCase( Locale.ROOT ).toLowerCase( Locale.ROOT );
		final String upperText = TEXT.toLowerCase( Locale.ROOT ).toUpperCase( Locale.ROOT );
		return Stream.of( "clobText", "nclobText", "longText", "longNationalizedText" )
				.flatMap( attribute -> {
					final String lowerExpression = "lower(t." + attribute + ")";
					final String upperExpression = "upper(t." + attribute + ")";
					return Stream.of(
							Arguments.of( "lower(coalesce(" + upperExpression + ", ''))",
									lowerText, "", "unselected", "%end" ),
							Arguments.of( "upper(coalesce(" + lowerExpression + ", ''))",
									upperText, "", "UNSELECTED", "%END" ),
							Arguments.of( "lower(case when t.id <> 3 then " + upperExpression + " else '' end)",
									lowerText, null, "", "%end" ),
							Arguments.of( "upper(case when t.id <> 3 then " + lowerExpression + " else '' end)",
									upperText, null, "", "%END" ),
							Arguments.of( "lower(case t.id when 3 then '' else " + upperExpression + " end)",
									lowerText, null, "", "%end" ),
							Arguments.of( "upper(case t.id when 3 then '' else " + lowerExpression + " end)",
									upperText, null, "", "%END" ),
							Arguments.of( "lower((select upper(s." + attribute
											+ ") from DB2CaseConversionText s where s.id = t.id))",
									lowerText, null, "unselected", "%end" ),
							Arguments.of( "upper((select lower(s." + attribute
											+ ") from DB2CaseConversionText s where s.id = t.id))",
									upperText, null, "UNSELECTED", "%END" )
					);
				} );
	}

	@Entity(name = "DB2CaseConversionText")
	static class Text {
		@Id
		int id;
		@JdbcTypeCode(SqlTypes.CLOB)
		String clobText;
		@JdbcTypeCode(SqlTypes.NCLOB)
		String nclobText;
		@JdbcTypeCode(SqlTypes.LONG32VARCHAR)
		@Column(length = 1_000_000)
		String longText;
		@JdbcTypeCode(SqlTypes.LONG32NVARCHAR)
		@Column(length = 1_000_000)
		String longNationalizedText;

		Text() {
		}

		Text(int id, String value) {
			this.id = id;
			this.clobText = value;
			this.nclobText = value;
			this.longText = value;
			this.longNationalizedText = value;
		}
	}
}
