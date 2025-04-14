/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslator;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests correct rendering of trim function emulation for {@link org.hibernate.dialect.AbstractTransactSQLDialect} dialects.
 *
 * @author Christian Beikov
 */
@ServiceRegistry
public class AnsiTrimEmulationFunctionTest  {
	private static final String trimSource = "a.column";
	private static final String LEADING = "substring(?1,patindex('%[^'+?2+']%',?1),len(?1+'x')-1-patindex('%[^'+?2+']%',?1)+1)";
	private static final String TRAILING = "substring(?1,1,len(?1+'x')-1-patindex('%[^'+?2+']%',reverse(?1))+1)";
	private static final String BOTH = "substring(?1,patindex('%[^'+?2+']%',?1),len(?1+'x')-1-patindex('%[^'+?2+']%',?1)-patindex('%[^'+?2+']%',reverse(?1))+2)";

	@Test
//	@RequiresDialect( SQLServerDialect.class )
	public void testBasicSqlServerProcessing(ServiceRegistryScope scope) {
		Dialect dialect = new SQLServerDialect();
		TrimFunction function = new TrimFunction( dialect, new TypeConfiguration() );

		performBasicSpaceTrimmingTests( dialect, scope.getRegistry(), function );

		// -> trim(LEADING '-' FROM a.column)
		String rendered = render( dialect, scope.getRegistry(), function, TrimSpec.LEADING, '-', trimSource );
		String expected = LEADING.replace( "?1", trimSource ).replace( "?2", "'-'" );
		assertEquals( expected, rendered );

		// -> trim(TRAILING '-' FROM a.column)
		rendered = render( dialect, scope.getRegistry(), function, TrimSpec.TRAILING, '-', trimSource );
		expected = TRAILING.replace( "?1", trimSource ).replace( "?2", "'-'" );
		assertEquals( expected, rendered );

		// -> trim(BOTH '-' FROM a.column)
		rendered = render( dialect, scope.getRegistry(), function, TrimSpec.BOTH, '-', trimSource );
		expected = BOTH.replace( "?1", trimSource ).replace( "?2", "'-'" );
		assertEquals( expected, rendered );
	}

	@Test
//	@RequiresDialect( SybaseDialect.class )
	public void testBasicSybaseProcessing(ServiceRegistryScope scope) {
		Dialect dialect = new SybaseDialect();
		TrimFunction function = new TrimFunction( dialect, new TypeConfiguration() );

		performBasicSpaceTrimmingTests( dialect, scope.getRegistry(), function );

		// -> trim(LEADING '-' FROM a.column)
		String rendered = render( dialect, scope.getRegistry(), function, TrimSpec.LEADING, '-', trimSource );
		String expected = LEADING.replace( "?1", trimSource ).replace( "?2", "'-'" );
		assertEquals( expected, rendered );

		// -> trim(TRAILING '-' FROM a.column)
		rendered = render( dialect, scope.getRegistry(), function, TrimSpec.TRAILING, '-', trimSource );
		expected = TRAILING.replace( "?1", trimSource ).replace( "?2", "'-'" );
		assertEquals( expected, rendered );

		// -> trim(BOTH '-' FROM a.column)
		rendered = render( dialect, scope.getRegistry(), function, TrimSpec.BOTH, '-', trimSource );
		expected = BOTH.replace( "?1", trimSource ).replace( "?2", "'-'" );
		assertEquals( expected, rendered );
	}

	private void performBasicSpaceTrimmingTests(Dialect dialect, StandardServiceRegistry registry, TrimFunction function) {
		// -> trim(a.column)
		String rendered = render( dialect, registry, function, TrimSpec.BOTH, ' ', trimSource );
		assertEquals( "ltrim(rtrim(a.column))", rendered );

		// -> trim(LEADING FROM a.column)
		rendered = render( dialect, registry, function, TrimSpec.LEADING, ' ', trimSource );
		assertEquals( "ltrim(a.column)", rendered );

		// -> trim(TRAILING FROM a.column)
		rendered = render( dialect, registry, function, TrimSpec.TRAILING, ' ', trimSource );
		assertEquals( "rtrim(a.column)", rendered );
	}

	private String render(
			Dialect dialect,
			StandardServiceRegistry registry,
			TrimFunction function,
			TrimSpec trimSpec,
			char trimCharacter,
			String trimSource) {
		SessionFactoryImplementor factory = Mockito.mock( SessionFactoryImplementor.class );
		JdbcServices jdbcServices = Mockito.mock( JdbcServices.class );
		Mockito.doReturn( jdbcServices ).when( factory ).getJdbcServices();
		Mockito.doReturn( registry ).when( factory ).getServiceRegistry();
		Mockito.doReturn( dialect ).when( jdbcServices ).getDialect();
		StandardSqlAstTranslator<JdbcOperation> walker = new StandardSqlAstTranslator<>(
				factory,
				null,
				null
		);
		List<SqlAstNode> sqlAstArguments = new ArrayList<>();
		sqlAstArguments.add( new TrimSpecification( trimSpec ) );
		sqlAstArguments.add( new QueryLiteral<>( trimCharacter, new BasicTypeImpl<>( CharacterJavaType.INSTANCE, CharJdbcType.INSTANCE ) ) );
		sqlAstArguments.add( new SelfRenderingExpression() {
			@Override
			public void renderToSql(
					SqlAppender sqlAppender, SqlAstTranslator<?> walker, SessionFactoryImplementor sessionFactory) {
				sqlAppender.appendSql( trimSource );
			}

			@Override
			public JdbcMappingContainer getExpressionType() {
				return null;
			}
		} );
		function.render( walker, sqlAstArguments, (ReturnableType<?>) null, walker );
		return walker.getSql();
	}

}
