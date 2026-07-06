/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Richard Barnes 4 May 2016
 */
@JiraKey(value = "HHH-3628")
public class HiloOptimizerConcurrencyTest {

	@Test
	public void testTwoSessionsSerialGeneration() {
		inSessionFactory( true, (sf1) -> {
			var session1 = sf1.openSession();

			TransactionUtil.inTransaction( session1, (SessionImplementor s) -> {
				var p = new HibPerson();
				session1.persist( p );
			} );

			inSessionFactory( false, (sf2) -> {
				var session2 = sf2.openSession();

				TransactionUtil.inTransaction( session2, (SessionImplementor s) -> {
					var p = new HibPerson();
					session2.persist( p );
				} );

				for ( int i = 2; i < 6; i++ ) {
					TransactionUtil.inTransaction( session1, (SessionImplementor s) -> {
						var p = new HibPerson();
						session1.persist( p );
					} );
				}

				TransactionUtil.inTransaction( session2, (SessionImplementor s) -> {
					var p = new HibPerson();
					session2.persist( p );
				} );

				TransactionUtil.inTransaction( session1, (SessionImplementor s) -> {
					var p = new HibPerson();
					session1.persist( p );
				} );
			} );
		} );
	}

	private void inSessionFactory(boolean createSchema, Consumer<SessionFactoryImplementor> action) {
		try (var serviceRegistry = createServiceRegistry( createSchema )) {
			final MetadataImplementor metadata = (MetadataImplementor) MetadataBuildingTestHelper.buildMetadata(
					serviceRegistry,
					HibPerson.class
			);
			try (var sessionFactory = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( metadata )) {
				action.accept(  sessionFactory );
			}
		}
	}

	private StandardServiceRegistry createServiceRegistry(boolean createSchema) {
		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		if ( createSchema ) {
			ssrb.applySetting( HBM2DDL_AUTO, Action.CREATE_DROP );
		}
		return ssrb.build();
	}

	@Entity(name = "HibPerson")
	public static class HibPerson {

		@Id
		@GenericGenerator(type = TableGenerator.class, parameters = {
				@Parameter(name = "table_name", value = "HIB_TGEN"),
				@Parameter(name = "prefer_entity_table_as_segment_value", value = "true"),
				@Parameter(name = "optimizer", value = "hilo"),
				@Parameter(name = "initial_value", value = "1"),
				@Parameter(name = "increment_size", value = "5")
		})
		private long id = -1;

		public HibPerson() {
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

	}
}
