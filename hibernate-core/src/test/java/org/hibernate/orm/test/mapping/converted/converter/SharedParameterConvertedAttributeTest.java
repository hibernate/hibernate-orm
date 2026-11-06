package org.hibernate.orm.test.mapping.converted.converter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		SharedParameterConvertedAttributeTest.ValidityInterval.class,
		SharedParameterConvertedAttributeTest.ConvertedFlags.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20955")
class SharedParameterConvertedAttributeTest {
	private static final OffsetDateTime NOW = OffsetDateTime.parse( "2023-05-03T12:00+02:00" );

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new ValidityInterval( 1, NOW.minusHours( 1 ), NOW.plusHours( 1 ) ) );
			session.persist( new ValidityInterval( 2, NOW.plusHours( 1 ), NOW.plusHours( 2 ) ) );
			session.persist( new ConvertedFlags( 1, true ) );
			session.persist( new ConvertedFlags( 2, false ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"r.validFrom <= :now",
			"r.validFrom <= :now and r.validTo > :now",
			"r.validFrom <= :now and r.validTo > :now and r.recordedFrom <= :now",
			"r.validFrom <= :now and r.validTo > :now and r.validFrom <= :now",
			"r.validFrom <= :now and r.validFrom <= :now and r.validFrom <= :now and r.validFrom <= :now",
			"r.validFrom <= :now and r.validTo > :now and r.recordedFrom <= :now and r.recordedTo > :now"
	})
	void testSharedParameter(String predicate, SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select r.id from ValidityInterval r where " + predicate,
				Integer.class
		).setParameter( "now", NOW.withOffsetSameInstant( ZoneOffset.ofHours( -5 ) ) )
				.getResultList() ).containsExactly( 1 ) );
	}

	@Test
	void testDistinctParameters(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select r.id from ValidityInterval r "
						+ "where r.validFrom <= :start and r.validTo > :end and r.recordedFrom <= :recorded",
				Integer.class
		).setParameter( "start", NOW )
				.setParameter( "end", NOW )
				.setParameter( "recorded", NOW )
				.getResultList() ).containsExactly( 1 ) );
	}

	@Test
	void testDifferentConvertersWithSameJdbcType(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select f.id from ConvertedFlags f "
						+ "where f.yesNo = :value and f.trueFalse = :value and f.yesNo = :value",
				Integer.class
		).setParameter( "value", true ).getResultList() ).containsExactly( 1 ) );
	}

	@Test
	void testDifferentJdbcTypes(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select f.id from ConvertedFlags f "
						+ "where f.yesNo = :value and f.numeric = :value and f.yesNo = :value",
				Integer.class
		).setParameter( "value", true ).getResultList() ).containsExactly( 1 ) );
	}

	@Entity(name = "ValidityInterval")
	static class ValidityInterval {
		@Id
		private Integer id;

		@Convert(converter = UtcOffsetConverter.class)
		private OffsetDateTime validFrom;

		@Convert(converter = UtcOffsetConverter.class)
		private OffsetDateTime validTo;

		@Convert(converter = UtcOffsetConverter.class)
		private OffsetDateTime recordedFrom;

		@Convert(converter = UtcOffsetConverter.class)
		private OffsetDateTime recordedTo;

		ValidityInterval() {
		}

		ValidityInterval(Integer id, OffsetDateTime from, OffsetDateTime to) {
			this.id = id;
			this.validFrom = from;
			this.validTo = to;
			this.recordedFrom = from;
			this.recordedTo = to;
		}
	}

	@Converter
	static class UtcOffsetConverter implements AttributeConverter<OffsetDateTime, LocalDateTime> {
		@Override
		public LocalDateTime convertToDatabaseColumn(OffsetDateTime attribute) {
			return attribute == null ? null : attribute.withOffsetSameInstant( ZoneOffset.UTC ).toLocalDateTime();
		}

		@Override
		public OffsetDateTime convertToEntityAttribute(LocalDateTime dbData) {
			return dbData == null ? null : dbData.atOffset( ZoneOffset.UTC );
		}
	}

	@Entity(name = "ConvertedFlags")
	static class ConvertedFlags {
		@Id
		private Integer id;

		@Convert(converter = YesNoConverter.class)
		private boolean yesNo;

		@Convert(converter = TrueFalseConverter.class)
		private boolean trueFalse;

		@Convert(converter = NumericBooleanConverter.class)
		private boolean numeric;

		ConvertedFlags() {
		}

		ConvertedFlags(Integer id, boolean value) {
			this.id = id;
			this.yesNo = value;
			this.trueFalse = value;
			this.numeric = value;
		}
	}
}
