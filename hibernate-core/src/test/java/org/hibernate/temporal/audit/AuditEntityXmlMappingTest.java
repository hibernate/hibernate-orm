package org.hibernate.temporal.audit;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.SharedSessionContract;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * XML-mapping counterpart to {@link AuditEntityTest}: the same
 * {@code @Audited} behavior, but the entity is mapped via the
 * Hibernate {@code mapping.xsd} XML format instead of annotations.
 */
@JiraKey("HHH-20617")
@AuditedTest
@SessionFactory
@DomainModel(
		annotatedClasses = AuditEntityXmlMappingTest.XmlAuditEntity.class,
		xmlMappings = "org/hibernate/temporal/audit/AuditEntityXmlMappingTest.xml"
)
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityXmlMappingTest$TxIdSupplier"))
class AuditEntityXmlMappingTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@Test
	void test(SessionFactoryScope scope) {
		currentTxId = 0;
		scope.getSessionFactory().inTransaction(
				session -> {
					XmlAuditEntity entity = new XmlAuditEntity();
					entity.id = 1L;
					entity.text = "hello";
					entity.stringSet.add( "hello" );
					session.persist( entity );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					XmlAuditEntity entity = session.find( XmlAuditEntity.class, 1L );
					entity.text = "goodbye";
					entity.stringSet.add( "goodbye" );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					XmlAuditEntity entity = session.find( XmlAuditEntity.class, 1L );
					session.remove( entity );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					XmlAuditEntity entity = session.find( XmlAuditEntity.class, 1L );
					assertNull( entity );
				}
		);
		try (var s = scope.getSessionFactory().withOptions().atChangeset( 0 ).open()) {
			XmlAuditEntity entity = s.find( XmlAuditEntity.class, 1L );
			assertNull( entity );
		}
		try (var s = scope.getSessionFactory().withOptions().atChangeset( 1 ).open()) {
			XmlAuditEntity entity = s.find( XmlAuditEntity.class, 1L );
			assertEquals( "hello", entity.text );
			assertEquals( Set.of( "hello" ), entity.stringSet );
			XmlAuditEntity result =
					s.createSelectionQuery( "from XmlAuditEntity where id = 1", XmlAuditEntity.class )
							.getSingleResultOrNull();
			assertSame( entity, result );
		}
		try (var s = scope.getSessionFactory().withOptions().atChangeset( 2 ).open()) {
			XmlAuditEntity entity = s.find( XmlAuditEntity.class, 1L );
			assertEquals( "goodbye", entity.text );
			assertEquals( Set.of( "hello", "goodbye" ), entity.stringSet );
		}
		try (var s = scope.getSessionFactory().withOptions().atChangeset( 3 ).open()) {
			XmlAuditEntity entity = s.find( XmlAuditEntity.class, 1L );
			assertNull( entity );
		}
	}

	// ---- Entity classes ----
	// Note: intentionally no @Audited/@Entity annotations here; the
	// mapping is entirely XML-driven (metadata-complete) via
	// AuditEntityXmlMappingTest.xml.

	public static class XmlAuditEntity {
		Long id;
		String text;
		int version;
		Set<String> stringSet = new HashSet<>();
	}
}
