/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static jakarta.persistence.GenerationType.AUTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		MultiLoadSubSelectCollectionDialectWithLimitTest.Parent.class,
		MultiLoadSubSelectCollectionDialectWithLimitTest.Child.class
})
@ServiceRegistry(
		settingProviders = @SettingProvider( provider = MultiLoadSubSelectCollectionDialectWithLimitTest.TestSettingProvider.class, settingName = AvailableSettings.DIALECT)
)
@SessionFactory(generateStatistics = true, useCollectingStatementInspector = true)
@RequiresDialect( value = H2Dialect.class )
public class MultiLoadSubSelectCollectionDialectWithLimitTest {


	public static class TestSettingProvider implements SettingProvider.Provider<String> {

		@Override
		public String getSetting() {
			return TestDialect.class.getName();
		}
	}

	public static class TestDialect extends H2Dialect {

		public TestDialect(DialectResolutionInfo info) {
			super( info );
		}

		public TestDialect() {
		}

		public TestDialect(DatabaseVersion version) {
			super( version );
		}

		@Override
		public int getInExpressionCountLimit() {
			return 50;
		}
	}

	@BeforeEach
	public void before(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setCacheMode( CacheMode.IGNORE );
			for ( int i = 1; i <= 60; i++ ) {
				final Parent p = new Parent( i, "Entity #" + i );
				for ( int j = 0; j < i; j++ ) {
					Child child = new Child();
					child.setParent( p );
					p.getChildren().add( child );
				}
				session.persist( p );
			}
		} );
	}

	@AfterEach
	public void after(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-12740")
	public void testSubselect(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					List<Parent> list = session.byMultipleIds( Parent.class ).multiLoad( ids( 56 ) );
					assertEquals( 56, list.size() );

					// None of the collections should be loaded yet
					if ( dialect.useArrayForMultiValuedParameters() ) {
						assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					}
					else {
						assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
					}
					for ( Parent p : list ) {
						assertFalse( Hibernate.isInitialized( p.children ) );
					}

					statementInspector.clear();

					// When the first collection is loaded multiple will be initialized
					Hibernate.initialize( list.get( 0 ).children );

					// exactly how depends on whether the Dialect supports use of SQL ARRAY
					if ( dialect.useArrayForMultiValuedParameters() ) {
						assertThat( Hibernate.isInitialized( list.get( 0 ).children ) ).isTrue();
						assertThat( Hibernate.isInitialized( list.get( 50 ).children ) ).isTrue();
						assertThat( Hibernate.isInitialized( list.get( 52 ).children ) ).isTrue();
						assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					}
					else {
						for ( int i = 0; i < 50; i++ ) {
							assertTrue( Hibernate.isInitialized( list.get( i ).children ) );
							assertEquals( i + 1, list.get( i ).children.size() );
						}

						// The collections for the 51st through 56th entities should still be uninitialized
						for ( int i = 50; i < 56; i++ ) {
							assertFalse( Hibernate.isInitialized( list.get( i ).children ) );
						}

						// When the 51st collection gets initialized, the remaining collections should
						// also be initialized.
						Hibernate.initialize( list.get( 50 ).children );

						for ( int i = 50; i < 56; i++ ) {
							assertTrue( Hibernate.isInitialized( list.get( i ).children ) );
							assertEquals( i + 1, list.get( i ).children.size() );
						}
					}
				}
		);
	}

	private Integer[] ids(int count) {
		Integer[] ids = new Integer[count];
		for ( int i = 1; i <= count; i++ ) {
			ids[i - 1] = i;
		}
		return ids;
	}

	@Entity(name = "Parent")
	@Table(name = "Parent")
	@BatchSize(size = 15)
	public static class Parent {
		Integer id;
		String text;
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@Fetch(FetchMode.SUBSELECT)
		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = AUTO)
		private int id;

		@ManyToOne(fetch = FetchType.LAZY, optional = true)
		private Parent parent;

		public Child() {
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public int getId() {
			return id;
		}
	}
}
