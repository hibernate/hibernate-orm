package org.hibernate.orm.test.version;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-10026" )
// This legacy version smoke test does not assert preservation of the original offset.
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel(annotatedClasses = OffsetDateTimeVersionTest.TheEntity.class)
@SessionFactory
public class OffsetDateTimeVersionTest {

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testInstantUsageAsVersion(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1 ) );
		} );

		factoryScope.inTransaction( (session) -> {
			var e = session.find( TheEntity.class, 1 );
			assertNotNull( e.getTs() );
		} );
	}


	@Entity(name = "TheEntity")
	@Table(name="the_entity")
	public static class TheEntity {
		private Integer id;
		private OffsetDateTime ts;

		public TheEntity() {
		}

		public TheEntity(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Version
		public OffsetDateTime getTs() {
			return ts;
		}

		public void setTs(OffsetDateTime ts) {
			this.ts = ts;
		}
	}
}
