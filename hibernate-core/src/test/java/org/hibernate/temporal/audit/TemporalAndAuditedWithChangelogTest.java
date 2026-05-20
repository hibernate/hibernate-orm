/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Temporal;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.DefaultChangelog;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AuditedTest
@SessionFactory
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"))
@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 11,
		versionMatchMode = VersionMatchMode.OLDER, reason = "See https://jira.mariadb.org/browse/MDEV-39230")
@DomainModel(annotatedClasses = {
		TemporalAndAuditedWithChangelogTest.TemporalProduct.class,
		TemporalAndAuditedWithChangelogTest.AuditedOrder.class,
		DefaultChangelog.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-20453")
class TemporalAndAuditedWithChangelogTest {

	@Temporal
	@Entity(name = "TemporalProduct")
	static class TemporalProduct {
		@Id
		long id;
		@Column(name = "name_col")
		String name;
		@Column(name = "price_col")
		double price;
	}

	@Audited
	@Entity(name = "AuditedOrder")
	static class AuditedOrder {
		@Id
		long id;
		@Column(name = "description_col")
		String description;
	}

	@Test
	void testTemporalAndAuditedCoexist(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction( session -> {
			var product = new TemporalProduct();
			product.id = 1L;
			product.name = "Widget";
			product.price = 10.0;
			session.persist( product );

			var order = new AuditedOrder();
			order.id = 1L;
			order.description = "First order";
			session.persist( order );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var product = session.find( TemporalProduct.class, 1L );
			product.price = 20.0;
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var order = session.find( AuditedOrder.class, 1L );
			order.description = "Updated order";
		} );

		// Verify current state
		scope.getSessionFactory().inTransaction( session -> {
			var product = session.find( TemporalProduct.class, 1L );
			assertEquals( 20.0, product.price );

			var order = session.find( AuditedOrder.class, 1L );
			assertEquals( "Updated order", order.description );
		} );

		// Verify temporal time-travel read at first changeset
		try (var session = scope.getSessionFactory().withOptions()
				.atChangeset( 1L ).open()) {
			var product = session.find( TemporalProduct.class, 1L );
			assertNotNull( product );
			assertEquals( "Widget", product.name );
			assertEquals( 10.0, product.price );
		}

		// Verify audit log for the audited order
		scope.getSessionFactory().inTransaction( session -> {
			var auditLog = AuditLogFactory.create( session );
			var changesetIds = auditLog.getChangesets( AuditedOrder.class, 1L );
			assertEquals( 2, changesetIds.size() );

			var revisions = session.createSelectionQuery(
					"from DefaultChangelog where id in :ids order by id",
					DefaultChangelog.class
			).setParameter( "ids", changesetIds ).getResultList();
			assertEquals( 2, revisions.size() );

			for ( var rev : revisions ) {
				assertTrue( rev.getTimestamp() > 0 );
				assertNotNull( rev.getRevisionInstant() );
			}
		} );
	}
}
