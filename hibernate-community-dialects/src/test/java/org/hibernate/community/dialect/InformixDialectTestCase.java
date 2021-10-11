/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.util.Collections;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.orm.test.jpa.JpaComplianceStub;
import org.hibernate.metamodel.model.domain.internal.JpaMetamodelImpl;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.internal.NamedObjectRepositoryImpl;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.DateJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Testing of patched support for Informix boolean type; see HHH-9894, HHH-10800
 *
 * @author Greg Jones
 */
public class InformixDialectTestCase extends BaseUnitTestCase {

	private static final InformixDialect dialect = new InformixDialect();
	private static ServiceRegistry ssr;
	private static QueryEngine queryEngine;

	@BeforeClass
	public static void init() {
		final JpaMetamodelImpl jpaMetamodel = new JpaMetamodelImpl( new TypeConfiguration(), new JpaComplianceStub() );

		ssr = new StandardServiceRegistryBuilder().build();
		queryEngine = new QueryEngine(
				null,
				null,
				jpaMetamodel,
				ValueHandlingMode.BIND,
				dialect.getPreferredSqlTypeCodeForBoolean(),
				false,
				new NamedObjectRepositoryImpl( Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap() ),
				null,
				dialect,
				ssr
		);
	}

	@AfterClass
	public static void tearDown() {
		queryEngine.close();
		ssr.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9894")
	public void testToBooleanValueStringTrue() {
		assertEquals( "'t'", dialect.toBooleanValueString( true ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9894")
	public void testToBooleanValueStringFalse() {
		assertEquals( "'f'", dialect.toBooleanValueString( false ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10800")
	public void testCurrentTimestampFunction() {
		SqmFunctionDescriptor functionDescriptor = queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor( "current_timestamp" );
		SelfRenderingSqmFunction<Object> sqmExpression = functionDescriptor.generateSqmExpression(
				null,
				queryEngine,
				new TypeConfiguration()
		);
		BasicType<?> basicType = (BasicType<?>) sqmExpression.getNodeType();
		assertEquals( DateJavaTypeDescriptor.INSTANCE, basicType.getJavaTypeDescriptor() );
		assertEquals( TimestampJdbcType.INSTANCE, basicType.getJdbcTypeDescriptor() );

		SqlAppender appender = new StringBuilderSqlAppender();
		sqmExpression.getRenderingSupport().render( appender, Collections.emptyList(), null );
		assertEquals( "current", appender.toString() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10800")
	public void testCurrentDateFunction() {
		SqmFunctionDescriptor functionDescriptor = queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor( "current_date" );
		SelfRenderingSqmFunction<Object> sqmExpression = functionDescriptor.generateSqmExpression(
				null,
				queryEngine,
				new TypeConfiguration()
		);
		BasicType<?> basicType = (BasicType<?>) sqmExpression.getNodeType();
		assertEquals( DateJavaTypeDescriptor.INSTANCE, basicType.getJavaTypeDescriptor() );
		assertEquals( DateJdbcType.INSTANCE, basicType.getJdbcTypeDescriptor() );

		SqlAppender appender = new StringBuilderSqlAppender();
		sqmExpression.getRenderingSupport().render( appender, Collections.emptyList(), null );
		assertEquals( "today", appender.toString() );
	}

	private static class StringBuilderSqlAppender implements SqlAppender {
		private final StringBuilder sb;

		public StringBuilderSqlAppender() {
			this.sb = new StringBuilder();
		}

		@Override
		public void appendSql(String fragment) {
			sb.append( fragment );
		}

		@Override
		public void appendSql(char fragment) {
			sb.append( fragment );
		}

		@Override
		public String toString() {
			return sb.toString();
		}
	}
}
