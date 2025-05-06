/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.secondarytable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.PHYSICAL_NAMING_STRATEGY, value = "org.hibernate.orm.test.annotations.secondarytable.SecondaryTableQuotingTest$TestNamingStrategy")
		}
)
@DomainModel(annotatedClasses = SecondaryTableQuotingTest.Foo.class)
@SessionFactory
@JiraKey(value = "HHH-6328")
public class SecondaryTableQuotingTest {

	@Test
	public void test(SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory().getRuntimeMetamodels().getMappingMetamodel()
				.getEntityDescriptor( Foo.class );
		final EntityTableMapping secondaryTableMapping = entityDescriptor.getTableMappings()[1];
		assertFalse( secondaryTableMapping.isOptional() );
	}

	@Entity(name = "Foo")
	@SecondaryTable(name = "bar", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "foo_id")})
	@SecondaryRow(table = "bar", optional = false)
	public static class Foo {
		@Id
		private Long id;
		private String name;
		@Column(name = "bar_value", table = "bar")
		private Long barValue;

	}

	public static class TestNamingStrategy extends PhysicalNamingStrategyStandardImpl {
		@Override
		public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return Identifier.toIdentifier( "TAB_" + logicalName.getText() );
		}
	}
}
