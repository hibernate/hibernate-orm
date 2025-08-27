/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SkipForDialect(dialectClass = MySQLDialect.class, majorVersion = 5, reason = "BLOB/TEXT column 'id' used in key specification without a key length")
@SkipForDialect(dialectClass = OracleDialect.class, reason = "ORA-02329: column of datatype LOB cannot be unique or a primary key")
@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix does not support unique / primary constraints on binary columns")
@DomainModel(
		annotatedClasses = {
				CollectionCacheEvictionComplexIdTest.Parent.class,
				CollectionCacheEvictionComplexIdTest.Child.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.AUTO_EVICT_COLLECTION_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true")
		}
)
public class CollectionCacheEvictionComplexIdTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent entity = new Parent();
					entity.id = new byte[] {
							(byte) ( 1 ),
							(byte) ( 2 ),
							(byte) ( 3 ),
							(byte) ( 4 )
					};
					entity.name = "Simple name";

					for ( int j = 1; j <= 2; j++ ) {
						Child child = new Child();
						child.id = j;
						entity.name = "Child name " + j;
						child.parent = entity;
						entity.children.add(child);
					}
					session.persist( entity );
				}
		);
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-7180")
	public void testEvict(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.setStatisticsEnabled( true );

		Parent parent = scope.fromTransaction(
				session -> session.createQuery( "from Parent p", Parent.class ).getSingleResult()
		);
		scope.inTransaction(
				session -> {
					Parent p = session.createQuery( "from Parent p", Parent.class ).getSingleResult();
					Child child1 = p.children.iterator().next();
					child1.name = "Updated child";
					child1.parent = parent;
				}
		);

		statistics.clear();
		scope.inTransaction(
				session -> {
					Parent p = session.createQuery( "from Parent p", Parent.class ).getSingleResult();
					final CollectionStatistics collectionStatistics = statistics.getCollectionStatistics(
							Parent.class.getName() + ".children" );
					assertEquals( 1, collectionStatistics.getCacheHitCount() );
					assertEquals( 0, collectionStatistics.getCacheMissCount() );
				}
		);

	}

	@Entity(name = "Parent")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Parent {
		@Id
		public byte[] id;
		public String name;
		@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		public Set<Child> children = new HashSet<>();
	}

	@Entity(name = "Child")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Child {
		@Id
		public Integer id;
		public String name;
		@ManyToOne(fetch = FetchType.LAZY)
		public Parent parent;

		public Child() {
		}

		public Child(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
		}
	}
}
