/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
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
		AuditTablePerClassExcludedPropertyTest.Base.class,
		AuditTablePerClassExcludedPropertyTest.Middle.class,
		AuditTablePerClassExcludedPropertyTest.Leaf.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.inheritance.AuditTablePerClassExcludedPropertyTest$TxIdSupplier"))
@Jira("https://hibernate.atlassian.net/browse/HHH-20857")
class AuditTablePerClassExcludedPropertyTest {
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
			var base = new Base();
			base.id = 1L;
			base.baseIncluded = "base";
			base.baseExcluded = "base-excluded";

			var middle = new Middle();
			middle.id = 2L;
			middle.baseIncluded = "middle";
			middle.baseExcluded = "middle-inherited";
			middle.middleIncluded = "middle";
			middle.middleExcluded = "middle-excluded";

			var leaf = new Leaf();
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

	@Test
	void testOrdinaryReadKeepsExcludedProperties(SessionFactoryScope scope) {
		scope.inSession( session -> {
			var results = session.createSelectionQuery( "from Base order by id", Base.class ).getResultList();
			assertThat( results ).hasSize( 3 );

			var base = results.get( 0 );
			assertThat( base.baseIncluded ).isEqualTo( "base" );
			assertThat( base.baseExcluded ).isEqualTo( "base-excluded" );

			var middle = (Middle) results.get( 1 );
			assertThat( middle.middleIncluded ).isEqualTo( "middle" );
			assertThat( middle.middleExcluded ).isEqualTo( "middle-excluded" );
			assertThat( middle.baseExcluded ).isEqualTo( "middle-inherited" );

			var leaf = (Leaf) results.get( 2 );
			assertThat( leaf.leafIncluded ).isEqualTo( "leaf" );
			assertThat( leaf.leafExcluded ).isEqualTo( "leaf-excluded" );
			assertThat( leaf.baseExcluded ).isEqualTo( "leaf-inherited" );
			assertThat( leaf.middleExcluded ).isEqualTo( "leaf-inherited-excluded" );
		} );
	}

	@Test
	void testAuditReadExcludesLocalAndInheritedProperties(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( AuditLog.ALL_CHANGESETS ).openStatelessSession()) {
			var results = session.createSelectionQuery( "from Base order by id", Base.class ).getResultList();
			assertThat( results ).hasSize( 3 );

			var base = results.get( 0 );
			assertThat( base.baseIncluded ).isEqualTo( "base" );
			assertThat( base.baseExcluded ).isNull();

			var middle = (Middle) results.get( 1 );
			assertThat( middle.baseIncluded ).isEqualTo( "middle" );
			assertThat( middle.middleIncluded ).isEqualTo( "middle" );
			assertThat( middle.middleExcluded ).isNull();
			assertThat( middle.baseExcluded ).isNull();

			var leaf = (Leaf) results.get( 2 );
			assertThat( leaf.baseIncluded ).isEqualTo( "leaf" );
			assertThat( leaf.middleIncluded ).isEqualTo( "leaf-middle" );
			assertThat( leaf.leafIncluded ).isEqualTo( "leaf" );
			assertThat( leaf.leafExcluded ).isNull();
			assertThat( leaf.baseExcluded ).isNull();
			assertThat( leaf.middleExcluded ).isNull();
		}
	}

	@Entity(name = "Base")
	@Audited
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static class Base {
		@Id
		long id;

		String baseIncluded;

		@Audited.Excluded
		String baseExcluded;

		protected Base() {
		}

	}

	@Entity(name = "Middle")
	static class Middle extends Base {
		String middleIncluded;

		@Audited.Excluded
		String middleExcluded;

		protected Middle() {
		}

	}

	@Entity(name = "Leaf")
	static class Leaf extends Middle {
		String leafIncluded;

		@Audited.Excluded
		String leafExcluded;

		protected Leaf() {
		}

	}
}
