/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.literal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.sqm.CastType;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractCriteriaLiteralHandlingModeTest extends EntityManagerFactoryBasedFunctionalTest {

	private SQLStatementInspector sqlStatementInspector;

	@Override
	protected void addConfigOptions(Map options) {
		sqlStatementInspector = new SQLStatementInspector();
		options.put( AvailableSettings.STATEMENT_INSPECTOR, sqlStatementInspector );
		options.put( AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, Boolean.FALSE );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Book.class
		};
	}

	@BeforeEach
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.id = 1;
			book.name = bookName();

			entityManager.persist( book );
		} );
	}

	@Test
	public void testLiteralHandlingMode() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();
			final CriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );

			final Root<Book> entity = query.from( Book.class );
			query.where(
				cb.and(
					cb.equal(
							entity.get( "id" ),
							1
					),
					cb.equal(
							entity.get( "name" ),
							bookName()
					)
				)
			);

			query.multiselect(
					cb.value( "abc" ),
					entity.get( "name" )
			);

			sqlStatementInspector.clear();

			List<Tuple> tuples = entityManager.createQuery( query ).getResultList();
			assertEquals( 1, tuples.size() );

			sqlStatementInspector.assertExecuted( expectedSQL() );
		} );
	}

	protected String casted(String expression, CastType castType) {
		final TypeConfiguration typeConfiguration = entityManagerFactory().unwrap( SessionFactoryImplementor.class ).getTypeConfiguration();
		return DialectContext.getDialect().castPattern( CastType.OTHER, castType )
				.replace(
						"?2",
						typeConfiguration.getDdlTypeRegistry().getDescriptor( SqlTypes.VARCHAR )
								.getCastTypeName(
										Size.nil(),
										typeConfiguration.getBasicTypeForJavaType( String.class ),
										typeConfiguration.getDdlTypeRegistry()
								)
				)
				.replace( "?1", expression );
	}

	protected abstract String expectedSQL();

	@Entity(name = "Book")
	public static class Book {

		@Id
		private Integer id;

		private String name;
	}

	protected String bookName() {
		return "Vlad's High-Performance Java Persistence";
	}
}
