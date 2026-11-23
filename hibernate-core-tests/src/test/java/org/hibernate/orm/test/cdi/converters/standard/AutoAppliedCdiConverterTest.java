package org.hibernate.orm.test.cdi.converters.standard;

import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.orm.test.cdi.converters.MonitorBean;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerLinker;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hibernate.cfg.ManagedBeanSettings.CDI_BEAN_MANAGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies CDI field injection into an automatically applied attribute converter.
///
/// @author Steve Ebersole
@JiraKey("HHH-13693")
@BaseUnitTest
public class AutoAppliedCdiConverterTest {
	@Test
	@ExtendWith(MonitorBean.Resetter.class)
	@CdiContainer(beanClasses = { MonitorBean.class, AutoAppliedConverter.class })
	@ServiceRegistry(resolvableSettings = @ServiceRegistry.ResolvableSetting(
			settingName = CDI_BEAN_MANAGER,
			resolver = CdiContainerLinker.StandardResolver.class
	))
	@DomainModel(annotatedClasses = { TestEntity.class, AutoAppliedConverter.class })
	@SessionFactory
	void testAutoAppliedConverterUsesInjectedBean(CdiContainerScope containerScope, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity( 1, "original" ) ) );
		assertTrue( MonitorBean.wasInstantiated() );
		assertEquals( 1, MonitorBean.currentToDbCount() );
		assertEquals( 0, MonitorBean.currentFromDbCount() );

		scope.inTransaction( session -> assertEquals(
				"converted:original",
				session.createNativeQuery( "select text from CdiAutoAppliedEntity", String.class ).getSingleResult()
		) );

		scope.inTransaction( session -> {
			final TestEntity entity = session.find( TestEntity.class, 1 );
			assertNotNull( entity );
			assertEquals( "original", entity.text );
		} );
		assertEquals( 1, MonitorBean.currentFromDbCount() );
		assertEquals( 1, MonitorBean.currentToDbCount() );
	}

	@Converter(autoApply = true)
	public static class AutoAppliedConverter implements AttributeConverter<String, String> {
		@Inject
		private MonitorBean monitor;

		@Override
		public String convertToDatabaseColumn(String attribute) {
			assertNotNull( monitor, "CDI must inject the auto-applied converter" );
			monitor.toDbCalled();
			return attribute == null ? null : "converted:" + attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			assertNotNull( monitor, "CDI must inject the auto-applied converter" );
			monitor.fromDbCalled();
			return dbData == null ? null : dbData.substring( "converted:".length() );
		}
	}

	@Entity(name = "CdiAutoAppliedEntity")
	public static class TestEntity {
		@Id
		private Integer id;
		private String text;

		public TestEntity() {
		}

		public TestEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
