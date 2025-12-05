/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.persister.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Marco Belladelli
 */
@ServiceRegistry(settings = {
		@Setting(name = GlobalTemporaryTableStrategy.CREATE_ID_TABLES, value = "false")
})
@DomainModel(annotatedClasses = {
		TemporaryTableStrategyTest.Bar.class
})
@SessionFactory
@RequiresDialect(OracleDialect.class)
public class TemporaryTableStrategyTest {

	@RegisterExtension
	public LoggerInspectionExtension logInspection =
			LoggerInspectionExtension.builder().setLogger(  Logger.getLogger( GlobalTemporaryTableStrategy.class ) ).build();

	private final Triggerable triggerable = logInspection.watchForLogMessages( Set.of(
			"Creating global-temp ID table",
			"Dropping global-temp ID table"
	) );

	@Test
	@JiraKey(value = "HHH-15550")
	public void testGlobalTemporaryTableStrategy(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Bar bar = new Bar();
			bar.id = 1;
			bar.name = "Noble";
			bar.name2 = "Experiment";

			s.persist( bar );

			assertFalse( triggerable.wasTriggered(), "Message was triggered" );
		} );
	}

	@Entity(name = "BAR")
	@Table(name = "BAR")
	@SecondaryTable(name = "BAR2")
	public static class Bar {
		@Id
		@Column(name = "ID")
		public Integer id;

		@Column(name = "name")
		public String name;

		@Column(name = "name2", table = "BAR2")
		public String name2;
	}
}
