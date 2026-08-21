/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Struct;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.PREFERRED_ARRAY_JDBC_TYPE,
				provider = OracleNestedTableSettingProvider.class
		)
)
@DomainModel(annotatedClasses = StructEmbeddableArrayEmptyTest.StructHolder.class)
@SessionFactory
@RequiresDialect( PostgreSQLDialect.class )
@RequiresDialect( OracleDialect.class )
public class StructEmbeddableArrayEmptyTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var entity = new StructHolder();
					entity.id = 1L;
					entity.myStruct = new MyStruct();
					entity.myStructs = new MyStruct[] { new MyStruct() };
					session.persist( entity );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testEmptyStructBehavesDifferently(SessionFactoryScope scope) {
		var loadedEntity = scope.fromTransaction(session -> session.find(StructHolder.class, 1));
		assertThat(loadedEntity.myStruct).isNotNull();
		assertThat(loadedEntity.myStructs).usingRecursiveComparison()
				.isEqualTo(new MyStruct[] { new MyStruct() })
				.isNotEqualTo(new MyStruct[] { null });
	}

	@Entity(name = "StructHolder")
	public static class StructHolder {
		@Id
		Long id;
		MyStruct myStruct;
		MyStruct[] myStructs;
	}

	@Embeddable
	@Struct(name = "MyStruct")
	public static class MyStruct {
		String field;
	}
}
