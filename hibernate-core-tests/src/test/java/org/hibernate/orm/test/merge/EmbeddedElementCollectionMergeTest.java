package org.hibernate.orm.test.merge;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = EmbeddedElementCollectionMergeTest.WeatherReport.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-14267")
class EmbeddedElementCollectionMergeTest {
	@Test
	void mergeNewEntityWithEmbeddedCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var report = new WeatherReport();
			report.id = 1L;
			report.measurements = new Measurements();
			report.measurements.readings = List.of( 1.0, 1.5 );
			session.merge( report );
		} );

		scope.inTransaction( session -> {
			var report = session.find( WeatherReport.class, 1L );
			assertThat( report.measurements.readings ).containsExactly( 1.0, 1.5 );
		} );
	}

	@Entity(name = "WeatherReport")
	static class WeatherReport {
		@Id
		Long id;

		@Embedded
		Measurements measurements;
	}

	@Embeddable
	static class Measurements {
		@ElementCollection
		@OrderColumn
		List<Double> readings;
	}
}
