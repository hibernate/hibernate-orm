package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.KEYWORD_AUTO_QUOTING_ENABLED, value = "true"))
@DomainModel(annotatedClasses = JsonEmbeddableKeywordNameTest.JsonHolder.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-21090")
public class JsonEmbeddableKeywordNameTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new JsonHolder( 1L, new Keywords( "abc", 2020, new Inner( "xyz" ) ) ) ) );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Keywords keywords = session.find( JsonHolder.class, 1L ).keywords;
			assertEquals( "abc", keywords.value );
			assertEquals( 2020, keywords.year );
			assertEquals( "xyz", keywords.user.day );
		} );
	}

	@Test
	public void testSelectMember(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals(
					"abc",
					session.createQuery( "select h.keywords.value from JsonHolder h", String.class ).getSingleResult()
			);
			assertEquals(
					2020,
					session.createQuery( "select h.keywords.year from JsonHolder h", Integer.class ).getSingleResult()
			);
			assertEquals(
					"xyz",
					session.createQuery( "select h.keywords.user.day from JsonHolder h", String.class ).getSingleResult()
			);
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonComponentUpdate.class)
	public void testUpdateMember(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "update JsonHolder h set h.keywords.value = :value, h.keywords.user.day = :day" )
					.setParameter( "value", "def" )
					.setParameter( "day", "uvw" )
					.executeUpdate();
			session.clear();
			final Keywords keywords = session.find( JsonHolder.class, 1L ).keywords;
			assertEquals( "def", keywords.value );
			assertEquals( 2020, keywords.year );
			assertEquals( "uvw", keywords.user.day );
		} );
	}

	@Entity(name = "JsonHolder")
	public static class JsonHolder {
		@Id
		private Long id;
		@JdbcTypeCode(SqlTypes.JSON)
		private Keywords keywords;

		public JsonHolder() {
		}

		public JsonHolder(Long id, Keywords keywords) {
			this.id = id;
			this.keywords = keywords;
		}
	}

	@Embeddable
	public static class Keywords {
		private String value;
		private Integer year;
		@JdbcTypeCode(SqlTypes.JSON)
		private Inner user;

		public Keywords() {
		}

		public Keywords(String value, Integer year, Inner user) {
			this.value = value;
			this.year = year;
			this.user = user;
		}
	}

	@Embeddable
	public static class Inner {
		private String day;

		public Inner() {
		}

		public Inner(String day) {
			this.day = day;
		}
	}
}
