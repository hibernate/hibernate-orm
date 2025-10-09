/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hibernate.cfg.BatchSettings.ORDER_INSERTS;
import static org.hibernate.cfg.BatchSettings.ORDER_UPDATES;
import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-16485")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = {
		InsertOrderingCircularDependencyFalsePositiveTest.Wrapper.class,
		InsertOrderingCircularDependencyFalsePositiveTest.Condition.class,
		InsertOrderingCircularDependencyFalsePositiveTest.SimpleCondition.class,
		InsertOrderingCircularDependencyFalsePositiveTest.Expression.class,
		InsertOrderingCircularDependencyFalsePositiveTest.ConstantExpression.class,
		InsertOrderingCircularDependencyFalsePositiveTest.Condition.class,
		InsertOrderingCircularDependencyFalsePositiveTest.CompoundCondition.class,
})
@SessionFactory
public class InsertOrderingCircularDependencyFalsePositiveTest implements ServiceRegistryProducer {
	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder.applySetting( ORDER_INSERTS, true )
				.applySetting( ORDER_UPDATES, true )
				.applySetting( STATEMENT_BATCH_SIZE, 50 )
				.applySetting( CONNECTION_PROVIDER, connectionProvider )
				.build();
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
		connectionProvider.stop();
	}

	@Test
	public void testBatching(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			connectionProvider.clear();
			// This should be persistable but currently reports that it might be circular
			session.persist(Wrapper.create());
		});
	}

	@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
	@Entity(name = "Wrapper")
	public static class Wrapper {
		@Id
		private String id;
		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		private Condition condition;
		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		private Set<ConstantExpression> constantExpressions;

		public Wrapper() {
		}

		public static Wrapper create() {
			final Wrapper w = new Wrapper();
			final CompoundCondition cc = new CompoundCondition();
			final SimpleCondition c1 = new SimpleCondition();
			final SimpleCondition c2 = new SimpleCondition();
			final ConstantExpression e1 = new ConstantExpression();
			final ConstantExpression e2 = new ConstantExpression();
			final ConstantExpression e3 = new ConstantExpression();
			final ConstantExpression e4 = new ConstantExpression();
			final ConstantExpression e5 = new ConstantExpression();
			w.id = "w";
			w.condition = cc;
			cc.id = "cc";
			cc.first = c1;
			cc.second = c2;
			c1.id = "c1";
			c1.left = e1;
			c1.right = e2;
			c2.id = "c2";
			c2.left = e3;
			c2.right = e4;
			e1.id = "e1";
			e1.value = "e1";
			e2.id = "e2";
			e2.value = "e2";
			e3.id = "e3";
			e3.value = "e3";
			e4.id = "e4";
			e4.value = "e4";
			e5.id = "e5";
			e5.value = "e5";
			w.constantExpressions = new HashSet<>();
			w.constantExpressions.add(e5);
			return w;
		}
	}

	@Entity(name = "Condition")
	@Table(name = "cond")
	public static abstract class Condition {
		@Id
		protected String id;

		public Condition() {
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "SimpleCondition")
	public static class SimpleCondition extends Condition {
		@OneToOne(cascade = CascadeType.ALL)
		private Expression left;
		@OneToOne(cascade = CascadeType.ALL)
		private Expression right;

		public SimpleCondition() {
		}
	}
	@Entity(name = "Expression")
	public static abstract class Expression {
		@Id
		protected String id;

		protected Expression() {
		}

	}
	@SuppressWarnings("unused")
	@Entity(name = "ConstantExpression")
	public static class ConstantExpression extends Expression {
		@Column(name = "val")
		private String value;

		public ConstantExpression() {
		}
	}
	@Entity(name = "CompoundCondition")
	public static class CompoundCondition extends Condition {
		@OneToOne(cascade = CascadeType.ALL)
		protected Condition first;
		@OneToOne(cascade = CascadeType.ALL)
		protected Condition second;

		public CompoundCondition() {
		}
	}
}
