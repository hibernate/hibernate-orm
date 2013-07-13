/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.sql;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.hibernate.QueryException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ordering.antlr.ColumnMapper;
import org.hibernate.sql.ordering.antlr.ColumnReference;
import org.hibernate.sql.ordering.antlr.SqlValueReference;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.Type;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class TemplateTest extends BaseUnitTestCase {
	private static final PropertyMapping PROPERTY_MAPPING = new PropertyMapping() {
		public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException {
			if ( "sql".equals( propertyName ) ) {
				return new String[] { "sql" };
			}
			else if ( "component".equals( propertyName ) ) {
				return new String[] { "comp_1", "comp_2" };
			}
			else if ( "component.prop1".equals( propertyName ) ) {
				return new String[] { "comp_1" };
			}
			else if ( "component.prop2".equals( propertyName ) ) {
				return new String[] { "comp_2" };
			}
			else if ( "property".equals( propertyName ) ) {
				return new String[] { "prop" };
			}
			throw new QueryException( "could not resolve property: " + propertyName );
		}

		public Type toType(String propertyName) throws QueryException {
			return null;
		}

		public String[] toColumns(String alias, String propertyName) throws QueryException {
			return new String[0];
		}

		public Type getType() {
			return null;
		}
	};

	private static final ColumnMapper MAPPER = new ColumnMapper() {
		public SqlValueReference[] map(String reference) {
			final String[] columnNames = PROPERTY_MAPPING.toColumns( reference );
			final SqlValueReference[] result = new SqlValueReference[ columnNames.length ];
			int i = 0;
			for ( final String columnName : columnNames ) {
				result[i] = new ColumnReference() {
					@Override
					public String getColumnName() {
						return columnName;
					}
				};
				i++;
			}
			return result;
		}
 	};

	private static final Dialect DIALECT = new HSQLDialect();

	private static final SQLFunctionRegistry FUNCTION_REGISTRY = new SQLFunctionRegistry( DIALECT, Collections.EMPTY_MAP );

	private static SessionFactoryImplementor SESSION_FACTORY = null; // Required for ORDER BY rendering.

	@BeforeClass
	public static void buildSessionFactory() {
		Configuration cfg = new Configuration();
		cfg.setProperty( AvailableSettings.DIALECT, DIALECT.getClass().getName() );
		ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
		SESSION_FACTORY = (SessionFactoryImplementor) cfg.buildSessionFactory( serviceRegistry );
	}

	@AfterClass
	public static void closeSessionFactory() {
		if ( SESSION_FACTORY != null ) {
			SESSION_FACTORY.close();
			SESSION_FACTORY = null;
		}
	}

	@Test
	public void testSqlExtractFunction() {
		String fragment = "extract( year from col )";
		String template = Template.renderWhereStringTemplate( fragment, Template.TEMPLATE, DIALECT, FUNCTION_REGISTRY );

		assertEquals( "extract(year from " + Template.TEMPLATE + ".col)", template );
	}

	@Test
	public void testSqlTrimFunction() {
		String fragment = "trim( col )";
		String template = Template.renderWhereStringTemplate( fragment, Template.TEMPLATE, DIALECT, FUNCTION_REGISTRY );
		assertEquals( "trim(" + Template.TEMPLATE + ".col)", template );

		fragment = "trim( from col )";
		template = Template.renderWhereStringTemplate( fragment, Template.TEMPLATE, DIALECT, FUNCTION_REGISTRY );
		assertEquals( "trim(from " + Template.TEMPLATE + ".col)", template );

		fragment = "trim( both from col )";
		template = Template.renderWhereStringTemplate( fragment, Template.TEMPLATE, DIALECT, FUNCTION_REGISTRY );
		assertEquals( "trim(both from " + Template.TEMPLATE + ".col)", template );

		fragment = "trim( leading from col )";
		template = Template.renderWhereStringTemplate( fragment, Template.TEMPLATE, DIALECT, FUNCTION_REGISTRY );
		assertEquals( "trim(leading from " + Template.TEMPLATE + ".col)", template );

		fragment = "trim( TRAILING from col )";
		template = Template.renderWhereStringTemplate( fragment, Template.TEMPLATE, DIALECT, FUNCTION_REGISTRY );
		assertEquals( "trim(TRAILING from " + Template.TEMPLATE + ".col)", template );

		fragment = "trim( 'b' from col )";
		template = Template.renderWhereStringTemplate( fragment, Template.TEMPLATE, DIALECT, FUNCTION_REGISTRY );
		assertEquals( "trim('b' from " + Template.TEMPLATE + ".col)", template );

		fragment = "trim( both 'b' from col )";
		template = Template.renderWhereStringTemplate( fragment, Template.TEMPLATE, DIALECT, FUNCTION_REGISTRY );
		assertEquals( "trim(both 'b' from " + Template.TEMPLATE + ".col)", template );
	}

	@Test
	public void testSQLReferences() {
		String fragment = "sql asc, sql desc";
		String template = doStandardRendering( fragment );

		assertEquals( Template.TEMPLATE + ".sql asc, " + Template.TEMPLATE + ".sql desc", template );
	}

	@Test
	public void testQuotedSQLReferences() {
		String fragment = "`sql` asc, `sql` desc";
		String template = doStandardRendering( fragment );

		assertEquals( Template.TEMPLATE + ".\"sql\" asc, " + Template.TEMPLATE + ".\"sql\" desc", template );
	}

	@Test
	public void testPropertyReference() {
		String fragment = "property asc, property desc";
		String template = doStandardRendering( fragment );

		assertEquals( Template.TEMPLATE + ".prop asc, " + Template.TEMPLATE + ".prop desc", template );
	}

	@Test
	public void testFunctionReference() {
		String fragment = "upper(sql) asc, lower(sql) desc";
		String template = doStandardRendering( fragment );

		assertEquals( "upper(" + Template.TEMPLATE + ".sql) asc, lower(" + Template.TEMPLATE + ".sql) desc", template );
	}

	@Test
	public void testQualifiedFunctionReference() {
		String fragment = "qual.upper(property) asc, qual.lower(property) desc";
		String template = doStandardRendering( fragment );

		assertEquals( "qual.upper(" + Template.TEMPLATE + ".prop) asc, qual.lower(" + Template.TEMPLATE + ".prop) desc", template );
	}

	@Test
	public void testDoubleQualifiedFunctionReference() {
		String fragment = "qual1.qual2.upper(property) asc, qual1.qual2.lower(property) desc";
		String template = doStandardRendering( fragment );

		assertEquals( "qual1.qual2.upper(" + Template.TEMPLATE + ".prop) asc, qual1.qual2.lower(" + Template.TEMPLATE + ".prop) desc", template );
	}

	@Test
	public void testFunctionWithPropertyReferenceAsParam() {
		String fragment = "upper(property) asc, lower(property) desc";
		String template = doStandardRendering( fragment );

		assertEquals( "upper(" + Template.TEMPLATE + ".prop) asc, lower(" + Template.TEMPLATE + ".prop) desc", template );
	}

	@Test
	public void testNestedFunctionReferences() {
		String fragment = "upper(lower(sql)) asc, lower(upper(sql)) desc";
		String template = doStandardRendering( fragment );

		assertEquals( "upper(lower(" + Template.TEMPLATE + ".sql)) asc, lower(upper(" + Template.TEMPLATE + ".sql)) desc", template );
	}

	@Test
	public void testComplexNestedFunctionReferences() {
		String fragment = "mod(mod(sql,2),3) asc";
		String template = doStandardRendering( fragment );

		assertEquals( "mod(mod(" + Template.TEMPLATE + ".sql, 2), 3) asc", template );
	}

	@Test
	public void testCollation() {
		String fragment = "`sql` COLLATE my_collation, `sql` COLLATE your_collation";
		String template = doStandardRendering( fragment );

		assertEquals( Template.TEMPLATE + ".\"sql\" collate my_collation, " + Template.TEMPLATE + ".\"sql\" collate your_collation", template );
	}

	@Test
	public void testCollationAndOrdering() {
		String fragment = "sql COLLATE my_collation, upper(prop) COLLATE your_collation asc, `sql` desc";
		String template = doStandardRendering( fragment );

		assertEquals( Template.TEMPLATE + ".sql collate my_collation, upper(" + Template.TEMPLATE + ".prop) collate your_collation asc, " + Template.TEMPLATE + ".\"sql\" desc", template );
	}

	@Test
	public void testComponentReferences() {
		String fragment = "component asc";
		String template = doStandardRendering( fragment );

		assertEquals( Template.TEMPLATE + ".comp_1 asc, " + Template.TEMPLATE + ".comp_2 asc", template );
	}

	@Test
	public void testComponentDerefReferences() {
		String fragment = "component.prop1 asc";
		String template = doStandardRendering( fragment );

		assertEquals( Template.TEMPLATE + ".comp_1 asc", template );
	}

	public String doStandardRendering(String fragment) {
		return Template.renderOrderByStringTemplate( fragment, MAPPER, SESSION_FACTORY, DIALECT, FUNCTION_REGISTRY );
	}
}
