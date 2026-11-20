package org.hibernate.temporal.audit.inheritance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.AfterClassTemplate;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the owner revision of a collection-only change of a subclass instance
 * records the subclass, when the collection is declared by the inheritance root.
 */
@AuditedTest
@Jira("https://hibernate.atlassian.net/browse/HHH-20641")
@DomainModel(annotatedClasses = {
		AuditInheritanceCollectionTest.SingleTableRoot.class,
		AuditInheritanceCollectionTest.SingleTableSub.class,
		AuditInheritanceCollectionTest.JoinedRoot.class,
		AuditInheritanceCollectionTest.JoinedSub.class,
		AuditInheritanceCollectionTest.UnionRoot.class,
		AuditInheritanceCollectionTest.UnionSub.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(
		name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.inheritance.AuditInheritanceCollectionTest$TxIdSupplier"
))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditInheritanceCollectionTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@BeforeClassTemplate
	void createData(SessionFactoryScope scope) {
		currentTxId = 0;
		scope.inTransaction( session -> {
			final var single = new SingleTableSub();
			single.detail = "detail";
			session.persist( init( single ) );
			final var joined = new JoinedSub();
			joined.detail = "detail";
			session.persist( init( joined ) );
			final var union = new UnionSub();
			union.detail = "detail";
			session.persist( init( union ) );
		} );
		scope.inTransaction( session -> {
			session.find( SingleTableRoot.class, 1L ).tags.add( "b" );
			session.find( JoinedRoot.class, 1L ).tags.add( "b" );
			session.find( UnionRoot.class, 1L ).tags.add( "b" );
		} );
	}

	private static <T extends Owner> T init(T owner) {
		owner.id = 1L;
		owner.tags.add( "a" );
		return owner;
	}

	@AfterClassTemplate
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void collectionOnlyChangeKeepsSubclass(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			for ( var rootClass : List.of( SingleTableRoot.class, JoinedRoot.class, UnionRoot.class ) ) {
				final var owner = auditLog.find( rootClass, 1L, 2 );
				assertThat( owner ).as( rootClass.getSimpleName() ).hasFieldOrPropertyWithValue( "detail", "detail" );
				assertThat( owner.tags ).as( rootClass.getSimpleName() ).containsExactlyInAnyOrder( "a", "b" );
			}
		}
	}

	@MappedSuperclass
	abstract static class Owner {
		@Id
		Long id;
		// Owned by each inheritance root
		@ElementCollection
		Set<String> tags = new HashSet<>();
	}

	@Audited
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Entity(name = "CollSingleTableRoot")
	static class SingleTableRoot extends Owner {
	}

	@Entity(name = "CollSingleTableSub")
	static class SingleTableSub extends SingleTableRoot {
		String detail;
	}

	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	@Entity(name = "CollJoinedRoot")
	static class JoinedRoot extends Owner {
	}

	@Entity(name = "CollJoinedSub")
	static class JoinedSub extends JoinedRoot {
		String detail;
	}

	@Audited
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Entity(name = "CollUnionRoot")
	static class UnionRoot extends Owner {
	}

	@Entity(name = "CollUnionSub")
	static class UnionSub extends UnionRoot {
		String detail;
	}
}
