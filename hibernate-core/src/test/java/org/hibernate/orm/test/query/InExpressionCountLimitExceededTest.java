/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@DomainModel(annotatedClasses = {InExpressionCountLimitExceededTest.MyEntity.class} )
@ServiceRegistry(
		settingProviders = @SettingProvider( provider = InExpressionCountLimitExceededTest.TestSettingProvider.class, settingName = AvailableSettings.DIALECT),
		settings = { @Setting(name=AvailableSettings.STATEMENT_BATCH_SIZE, value = "60") }
)
@SessionFactory()
@RequiresDialect( value = H2Dialect.class )
@JiraKey(value = "HHH-16868")
public class InExpressionCountLimitExceededTest {

	public static class TestSettingProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return TestDialect.class.getName();
		}
	}

	public static class TestDialect extends H2Dialect {
		@Override
		public int getInExpressionCountLimit() {
			return 15;
		}
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void verifyThatSplitInClausesAreSurroundedByParenthesis(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 1; i <= 60; i++ ) {
						final MyEntity e = new MyEntity( i, i%3 == 0 ? "EveryThirdEntity #" + i : "Entity #" + i );
						session.persist( e );
					}
				}
		);
		List<Integer> selectedIds = new ArrayList<>();
		for ( int i = 1; i <= 60; i++ ) {
			if ( i%2 == 0) {
				selectedIds.add( i );
			}
		}
		scope.inTransaction(
				session -> {
					Query<MyEntity> q = session.createQuery( "select e from MyEntity e where e.id in ?1 and e.text like 'EveryThirdEntity%'", MyEntity.class );
					q.setParameterList(1, selectedIds);
					List<MyEntity> results = q.getResultList();
					assertEquals(10, results.size());
					for (MyEntity e : results) {
						assertEquals( e.getId()%6, 0 );
						assertEquals( "EveryThirdEntity", e.getText().split( " " )[0] );
					}
				}
		);
	}
	@Entity(name = "MyEntity")
	@Table(name = "MyEntity")
	public static class MyEntity {
		@Id
		Integer id;
		String text;

		public MyEntity() {
		}

		public MyEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

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
	}

}
