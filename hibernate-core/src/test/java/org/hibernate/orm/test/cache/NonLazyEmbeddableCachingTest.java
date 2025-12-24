/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.sql.Blob;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				NonLazyEmbeddableCachingTest.RootEntity.class, NonLazyEmbeddableCachingTest.LobEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
		}
)
@SessionFactory(generateStatistics = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-12613")
@BytecodeEnhanced(runNotEnhancedAsWell = true)
public class NonLazyEmbeddableCachingTest {

	@Test
	public void testFind(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		scope.inTransaction(
				session -> {
					statistics.clear();
					RootEntity rootEntity = new RootEntity();
					LobEntity lobEntity = new LobEntity();
					lobEntity.getData().setBlob(session.getLobCreator().createBlob("TEST".getBytes()));
					rootEntity.setLobEntity(lobEntity);

					session.persist( rootEntity );
					session.persist( lobEntity );
					session.flush();

					assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 1 );
				}
		);
	}

	@Entity
	public static class RootEntity {

		@Id
		@GeneratedValue
		private int id;
		private String name;
		@ManyToOne(fetch = FetchType.LAZY)
		private LobEntity lobEntity;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public LobEntity getLobEntity() {
			return lobEntity;
		}

		public void setLobEntity(LobEntity lobEntity) {
			this.lobEntity = lobEntity;
		}
	}

	@Entity
	@Cacheable
	public static class LobEntity {
		@Id
		@GeneratedValue
		private int id;
		@Embedded
		private DataHolder data = new DataHolder();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public DataHolder getData() {
			if (data == null) {
				data = new DataHolder();
			}
			return data;
		}
	}

	@Embeddable
	public static class DataHolder {

		@Lob
		@Basic(fetch = FetchType.LAZY)
		private Blob blob;

		public Blob getBlob() {
			return blob;
		}

		public void setBlob(Blob blob) {
			this.blob = blob;
		}
	}

}
