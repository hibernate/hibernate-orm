/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.annotations.CollectionBinder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ordering.antlr.ColumnMapper;
import org.hibernate.sql.ordering.antlr.ColumnReference;
import org.hibernate.sql.ordering.antlr.OrderByFragmentTranslator;
import org.hibernate.sql.ordering.antlr.OrderByTranslation;
import org.hibernate.sql.ordering.antlr.SqlValueReference;
import org.hibernate.sql.ordering.antlr.TranslationContext;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class OrderByAnnotationTests extends BaseNonConfigCoreFunctionalTestCase {
	private static final String ELEMENT_TOKEN = "$element$";
	private static final String TABLE_ALIAS = "a";
	private static final String COLUMN_NAME = "name";

	@Test
	public void testOrderByEmpty() {
		assertThat( translate( "" ), CoreMatchers.is( TABLE_ALIAS + '.' + COLUMN_NAME + " asc" ) );
	}

	@Test
	public void testOrderByJustDesc() {
		assertThat( translate( "desc" ), CoreMatchers.is( TABLE_ALIAS + '.' + COLUMN_NAME + " desc" ) );

		assertThat( translate( "DESC"), CoreMatchers.is( TABLE_ALIAS + '.' + COLUMN_NAME + " desc" ) );
	}

	@Test
	public void testOrderByJustAsc() {
		assertThat( translate( "asc"), CoreMatchers.is( TABLE_ALIAS + '.' + COLUMN_NAME + " asc" ) );

		assertThat( translate( "ASC"), CoreMatchers.is( TABLE_ALIAS + '.' + COLUMN_NAME + " asc" ) );
	}

	private String translate(String fragment) {
		fragment = CollectionBinder.adjustUserSuppliedValueCollectionOrderingFragment( fragment );

		final TranslationContext translationContext = translationContext();
		final OrderByTranslation translation = OrderByFragmentTranslator.translate( translationContext, fragment );

		return translation.injectAliases( columnReference -> TABLE_ALIAS );
	}

	private TranslationContext translationContext() {
		final ColumnMapper columnMapper = reference -> {
			assert ELEMENT_TOKEN.equals( reference );
			return new SqlValueReference[] {
					(ColumnReference) () -> COLUMN_NAME
			};
		};

		return new TranslationContext() {
			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return sessionFactory();
			}

			@Override
			public Dialect getDialect() {
				return getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect();
			}

			@Override
			public SQLFunctionRegistry getSqlFunctionRegistry() {
				return getSessionFactory().getSqlFunctionRegistry();
			}

			@Override
			public ColumnMapper getColumnMapper() {
				return columnMapper;
			}
		};
	}

//	@Override
//	protected void applyMetadataSources(MetadataSources metadataSources) {
//		super.applyMetadataSources( metadataSources );
//		metadataSources.addAnnotatedClass( A.class );
//	}
//
//	@Entity( name = "A" )
//	@Table( name = "T_A" )
//	public static class A {
//		@Id
//		public Integer id;
//		@ElementCollection
//		@Column( name = "name" )
//		@OrderBy
//		public List<String> names;
//	}
}
