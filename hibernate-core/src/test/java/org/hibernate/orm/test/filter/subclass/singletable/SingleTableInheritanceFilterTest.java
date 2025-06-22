/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass.singletable;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.orm.test.filter.AbstractStatefulStatelessFilterTest;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Jan Schatteman
 */
@JiraKey( value = "HHH-16435")
@DomainModel(
		annotatedClasses = {
				SingleTableInheritanceFilterTest.AbstractSuperClass.class,
				SingleTableInheritanceFilterTest.ChildEntityOne.class,
				SingleTableInheritanceFilterTest.ChildEntityTwo.class
		}
)
public class SingleTableInheritanceFilterTest extends AbstractStatefulStatelessFilterTest {

	@AfterEach
	public void cleanup() {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	public void testFilterAppliedOnSingleTableInheritance(
			BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		scope.inTransaction(
				s -> {
					s.persist( new ChildEntityOne() );
					s.persist( new ChildEntityTwo() );
				}
		);

		// test update
		inTransaction.accept(
				scope,
				s -> {
					s.enableFilter( "dummy_filter" );
					MutationQuery updateQuery = s.createMutationQuery( "update ChildEntityTwo cet set cet.name = 'John'");
					int updated = updateQuery.executeUpdate();
					assertEquals(1, updated);

					Query<AbstractSuperClass> query = s.createQuery( "select p from AbstractSuperClass p where p.name = 'John'", AbstractSuperClass.class);
					assertEquals(1, query.list().size() );
				}
		);

		// test delete
		inTransaction.accept(
				scope,
				s -> {
					s.enableFilter( "dummy_filter" );
					MutationQuery deleteQuery = s.createMutationQuery( "delete from ChildEntityTwo");
					int deleted = deleteQuery.executeUpdate();
					assertEquals(1, deleted);

					Query<AbstractSuperClass> query = s.createQuery( "select p from AbstractSuperClass p", AbstractSuperClass.class);
					assertEquals(1, query.list().size() );
				}
		);
	}

	@Entity(name = "AbstractSuperClass")
	@DiscriminatorColumn(name = "DISC_COL", discriminatorType = DiscriminatorType.INTEGER)
	@FilterDef(name = "dummy_filter", defaultCondition = "(id IS NOT NULL)")
	@Filter(name = "dummy_filter")
	public static abstract class AbstractSuperClass {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Integer id;

		String name;
	}

	@Entity(name = "ChildEntityOne")
	@DiscriminatorValue("1")
	public static class ChildEntityOne extends AbstractSuperClass {}

	@Entity(name = "ChildEntityTwo")
	@DiscriminatorValue("2")
	public static class ChildEntityTwo extends AbstractSuperClass {}

}
