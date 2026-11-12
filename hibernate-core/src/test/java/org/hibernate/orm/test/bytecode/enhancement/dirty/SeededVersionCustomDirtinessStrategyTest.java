/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@JiraKey("HHH-20757")
@DomainModel(annotatedClasses = SeededVersionCustomDirtinessStrategyTest.Document.class)
@ServiceRegistry(settingProviders = @SettingProvider(
		settingName = AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY,
		provider = SeededVersionCustomDirtinessStrategyTest.StrategyProvider.class
))
@SessionFactory(useCollectingStatementInspector = true)
@BytecodeEnhanced
public class SeededVersionCustomDirtinessStrategyTest {
	@Test
	void firstFlushAndSubsequentUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Document document = new Document();
			document.id = 1L;
			document.setName( "initial" );
			assertInstanceOf( SelfDirtinessTracker.class, document );
			final var statements = scope.getCollectingStatementInspector();
			statements.clear();

			// An assigned identifier keeps the INSERT queued until flush.
			// Version seeding reinjects values and marks the enhanced version dirty.
			session.persist( document );
			session.flush();
			assertEquals( 0L, document.version );
			statements.assertExecutedCount( 1 );
			statements.assertIsInsert( 0 );

			statements.clear();
			session.flush();
			statements.assertExecutedCount( 0 );

			document.setName( "changed" );
			session.flush();
			assertEquals( 1L, document.version );
			statements.assertExecutedCount( 1 );
			statements.assertIsUpdate( 0 );

			statements.clear();
		} );
		scope.getCollectingStatementInspector().assertExecutedCount( 0 );
		scope.inTransaction( session -> {
			final Document reloaded = session.find( Document.class, 1L );
			assertEquals( "changed", reloaded.name );
			assertEquals( 1L, reloaded.version );
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	public static class StrategyProvider implements SettingProvider.Provider<SnapshotStrategy> {
		@Override
		public SnapshotStrategy getSetting() {
			return new SnapshotStrategy();
		}
	}

	public static class SnapshotStrategy implements CustomEntityDirtinessStrategy {
		@Override
		public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) {
			return entity instanceof Document;
		}

		@Override
		public boolean isDirty(Object entity, EntityPersister persister, Session session) {
			return true;
		}

		@Override
		public void resetDirty(Object entity, EntityPersister persister, Session session) {
		}

		@Override
		public void findDirty(
				Object entity,
				EntityPersister persister,
				Session session,
				DirtyCheckContext context) {
			context.doDirtyChecking(
					attribute -> !Objects.equals( attribute.getCurrentValue(), attribute.getLoadedValue() )
			);
		}
	}

	@Entity(name = "SeededVersionStrategyDocument")
	public static class Document {
		@Id
		Long id;
		@Version
		Long version;
		String name;

		public void setName(String name) {
			this.name = name;
		}
	}
}
