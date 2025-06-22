/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import org.hibernate.cfg.Environment;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@JiraKey(value = "HHH-16485")
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class InsertOrderingCircularDependencyFalsePositiveTest extends BaseNonConfigCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
			Wrapper.class,
			Condition.class,
			SimpleCondition.class,
			Expression.class,
			ConstantExpression.class,
			Condition.class,
			CompoundCondition.class,
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put(Environment.ORDER_INSERTS, "true");
		settings.put(Environment.ORDER_UPDATES, "true");
		settings.put(Environment.STATEMENT_BATCH_SIZE, "50");
		settings.put(
			org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
			connectionProvider
		);
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Override
	protected boolean rebuildSessionFactoryOnError() {
		return false;
	}

	@Test
	public void testBatching() throws SQLException {
		doInHibernate(this::sessionFactory, session -> {
			connectionProvider.clear();
			// This should be persistable but currently reports that it might be circular
			session.persist(Wrapper.create());
		});
	}

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
