/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.filter.subclass.singletable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.filter.AbstractStatefulStatelessFilterTest;
import org.hibernate.query.MutationQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel(
		annotatedClasses = {
				SingleTableDefaultSchemaFilterTest.AbstractSuperClass.class,
				SingleTableDefaultSchemaFilterTest.ChildEntityOne.class,
				SingleTableDefaultSchemaFilterTest.ChildEntityTwo.class
		}
)
@ServiceRegistry( settings = @Setting( name = AvailableSettings.DEFAULT_SCHEMA, value = "public" ) )
@RequiresDialect( H2Dialect.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16661" )
public class SingleTableDefaultSchemaFilterTest extends AbstractStatefulStatelessFilterTest {
	@BeforeEach
	public void setUp() {
		scope.inTransaction( session -> {
			session.persist( new ChildEntityOne() );
			session.persist( new ChildEntityTwo() );
		} );
	}

	@AfterEach
	public void cleanup() {
		scope.inTransaction(
				session -> session.createMutationQuery( "delete from AbstractSuperClass" ).executeUpdate()
		);
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	public void testUpdate(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "dummy_filter" );
			final MutationQuery updateQuery = session.createMutationQuery(
					"update ChildEntityTwo cet set cet.name = 'John'" );
			final int updated = updateQuery.executeUpdate();
			assertEquals( 1, updated );
		} );
		inTransaction.accept( scope, session -> {
			session.enableFilter( "dummy_filter" );
			final List<AbstractSuperClass> resultList = session.createQuery(
					"select p from AbstractSuperClass p where p.name = 'John'",
					AbstractSuperClass.class
			).getResultList();
			assertEquals( 1, resultList.size() );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	public void testDelete(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "dummy_filter" );
			final MutationQuery deleteQuery = session.createMutationQuery( "delete from ChildEntityOne" );
			final int deleted = deleteQuery.executeUpdate();
			assertEquals( 1, deleted );
		} );
		inTransaction.accept( scope, session -> {
			session.enableFilter( "dummy_filter" );
			final List<AbstractSuperClass> resultList = session.createQuery(
					"select p from AbstractSuperClass p",
					AbstractSuperClass.class
			).getResultList();
			assertEquals( 1, resultList.size() );
		} );
	}

	@Entity( name = "AbstractSuperClass" )
	@DiscriminatorColumn( name = "disc_col", discriminatorType = DiscriminatorType.INTEGER )
	@FilterDef( name = "dummy_filter", defaultCondition = "(id is not null)" )
	@Filter( name = "dummy_filter" )
	public static abstract class AbstractSuperClass {
		@Id
		@GeneratedValue
		Integer id;
		String name;
	}

	@Entity( name = "ChildEntityOne" )
	@DiscriminatorValue( "1" )
	public static class ChildEntityOne extends AbstractSuperClass {
	}

	@Entity( name = "ChildEntityTwo" )
	@DiscriminatorValue( "2" )
	public static class ChildEntityTwo extends AbstractSuperClass {
	}
}
