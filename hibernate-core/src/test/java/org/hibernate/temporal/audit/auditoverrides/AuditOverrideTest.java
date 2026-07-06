/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.auditoverrides;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.AuditOverride;
import org.hibernate.annotations.AuditOverrides;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
try (var s = scope.getSessionFactory().withOptions().atChangeset( 200 ).open()) {
			//this call returns a record although auditing isn't event enabled
			//How can we verify that it was not audited?
			// what's the api for browsing the audit log?
//			assertNull( s.find( OverridingSubClass.class, 1L ) );
		}
 */


//@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditOverrideTest.AuditedEntity.class,
		AuditOverrideTest.SubClassWithEmptyAuditOverridesAnnotation.class,
		AuditOverrideTest.SubClassWithOverrideThatDoesntDisableTheAuditing.class,
		AuditOverrideTest.SubClassWithOverrideThatDisablesAuditing.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class AuditOverrideTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@Audited
	@MappedSuperclass
	static class AuditedEntity {
		@Id
		long id;
	}

//	@AuditOverride(name = "value", isAudited = false),

	@Entity
	@AuditOverrides({})
	static class SubClassWithEmptyAuditOverridesAnnotation extends AuditedEntity{
	}

	@BeforeClassTemplate
	void setupData(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction( session -> {
		} );
	}

	@Test
	public void emptyOverrides(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			var overrider = new SubClassWithEmptyAuditOverridesAnnotation();
			overrider.id = 1L;
			em.persist( overrider );
		} );

		assertTrue( AuditLogFactory.create( scope.getSessionFactory() ).isAudited( SubClassWithEmptyAuditOverridesAnnotation.class ) );
	}

	@Entity
	@AuditOverrides({@AuditOverride(forClass = AuditedEntity.class, isAudited = true)})
	static class SubClassWithOverrideThatDoesntDisableTheAuditing extends AuditedEntity{
	}

	@Test
	public void noopOverride(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			var overrider = new SubClassWithOverrideThatDoesntDisableTheAuditing();
			overrider.id = 1L;
			em.persist( overrider );
		} );

		assertTrue( AuditLogFactory.create( scope.getSessionFactory() ).isAudited( SubClassWithOverrideThatDoesntDisableTheAuditing.class ) );
	}

	@Entity
	@AuditOverrides({@AuditOverride(forClass = AuditedEntity.class, isAudited = false)})
	static class SubClassWithOverrideThatDisablesAuditing extends AuditedEntity{
	}

	@Test
	public void disablingOverride(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			var overrider = new SubClassWithOverrideThatDisablesAuditing();
			overrider.id = 1L;
			em.persist( overrider );
		} );

		assertFalse( AuditLogFactory.create( scope.getSessionFactory() ).isAudited( SubClassWithOverrideThatDisablesAuditing.class ) );
	}

}
