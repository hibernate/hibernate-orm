package org.hibernate.temporal.audit.inheritance.exclusion;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
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

import static org.assertj.core.api.Assertions.assertThat;

@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditJoinedExcludedPropertyTest.JoinedBase.class,
		AuditJoinedExcludedPropertyTest.JoinedMiddle.class,
		AuditJoinedExcludedPropertyTest.JoinedLeaf.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.inheritance.exclusion.AuditJoinedExcludedPropertyTest$TxIdSupplier"))
@Jira("https://hibernate.atlassian.net/browse/HHH-20857")
class AuditJoinedExcludedPropertyTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@BeforeClassTemplate
	void createTestData(SessionFactoryScope scope) {
		currentTxId = 0;
		scope.inTransaction( session -> {
			var base = new JoinedBase();
			base.id = 1L;
			base.baseIncluded = "base";
			base.baseExcluded = "base-excluded";

			var middle = new JoinedMiddle();
			middle.id = 2L;
			middle.baseIncluded = "middle";
			middle.baseExcluded = "middle-inherited";
			middle.middleIncluded = "middle";
			middle.middleExcluded = "middle-excluded";

			var leaf = new JoinedLeaf();
			leaf.id = 3L;
			leaf.baseIncluded = "leaf";
			leaf.baseExcluded = "leaf-inherited";
			leaf.middleIncluded = "leaf-middle";
			leaf.middleExcluded = "leaf-inherited-excluded";
			leaf.leafIncluded = "leaf";
			leaf.leafExcluded = "leaf-excluded";

			session.persist( base );
			session.persist( middle );
			session.persist( leaf );
		} );
	}

	@AfterClassTemplate
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testAuditRead(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( AuditLog.ALL_CHANGESETS ).openStatelessSession()) {
			var results = session.createSelectionQuery(
					"from JoinedBase order by id",
					JoinedBase.class
			).getResultList();
			assertThat( results ).hasSize( 3 );

			var base = (JoinedBase) results.get( 0 );
			assertThat( base.baseIncluded ).isEqualTo( "base" );
			assertThat( base.baseExcluded ).isNull();

			var middle = (JoinedMiddle) results.get( 1 );
			assertThat( middle.baseIncluded ).isEqualTo( "middle" );
			assertThat( middle.middleIncluded ).isEqualTo( "middle" );
			assertThat( middle.baseExcluded ).isNull();
			assertThat( middle.middleExcluded ).isNull();

			var leaf = (JoinedLeaf) results.get( 2 );
			assertThat( leaf.baseIncluded ).isEqualTo( "leaf" );
			assertThat( leaf.middleIncluded ).isEqualTo( "leaf-middle" );
			assertThat( leaf.leafIncluded ).isEqualTo( "leaf" );
			assertThat( leaf.baseExcluded ).isNull();
			assertThat( leaf.middleExcluded ).isNull();
			assertThat( leaf.leafExcluded ).isNull();
		}
	}

	@Test
	void testAuditReadWithTypePredicate(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( AuditLog.ALL_CHANGESETS ).openStatelessSession()) {
			var results = session.createSelectionQuery(
					"from JoinedBase b where type(b) = JoinedLeaf order by id",
					JoinedBase.class
			).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ) ).isInstanceOf( JoinedLeaf.class );
			var leaf = (JoinedLeaf) results.get( 0 );
			assertThat( leaf.baseIncluded ).isEqualTo( "leaf" );
			assertThat( leaf.middleIncluded ).isEqualTo( "leaf-middle" );
			assertThat( leaf.leafIncluded ).isEqualTo( "leaf" );
			assertThat( leaf.baseExcluded ).isNull();
			assertThat( leaf.middleExcluded ).isNull();
			assertThat( leaf.leafExcluded ).isNull();
		}
	}

	@Entity(name = "JoinedBase")
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	static class JoinedBase {
		@Id
		long id;

		String baseIncluded;

		@Audited.Excluded
		String baseExcluded;

		protected JoinedBase() {
		}

	}

	@Entity(name = "JoinedMiddle")
	static class JoinedMiddle extends JoinedBase {
		String middleIncluded;

		@Audited.Excluded
		String middleExcluded;

		protected JoinedMiddle() {
		}

	}

	@Entity(name = "JoinedLeaf")
	static class JoinedLeaf extends JoinedMiddle {
		String leafIncluded;

		@Audited.Excluded
		String leafExcluded;

		protected JoinedLeaf() {
		}

	}
}
