/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.hql;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;
import org.junit.Test;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.internal.ast.HqlParser;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 */
@FailureExpectedWithNewMetamodel
public class EJBQLTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[]{
			"hql/Animal.hbm.xml",
			"batchfetch/ProductLine.hbm.xml",
			"cid/Customer.hbm.xml",
			"cid/Order.hbm.xml",
			"cid/LineItem.hbm.xml",
			"cid/Product.hbm.xml",
			"legacy/Glarch.hbm.xml",
			"legacy/Fee.hbm.xml",
			"legacy/Qux.hbm.xml",
			"legacy/Fum.hbm.xml",
			"legacy/Holder.hbm.xml",
			"legacy/One.hbm.xml",
			"legacy/FooBar.hbm.xml",
			"legacy/Many.hbm.xml",
			"legacy/Baz.hbm.xml",
			"legacy/Simple.hbm.xml",
			"legacy/Middle.hbm.xml",
			"legacy/Category.hbm.xml",
			"legacy/Multi.hbm.xml",
			"legacy/Commento.hbm.xml",
			"legacy/Marelo.hbm.xml",
			"compositeelement/Parent.hbm.xml",
			"legacy/Container.hbm.xml",
		};
	}

	@Override
	public boolean createSchema() {
		return false;
	}

	@Test
	public void testEjb3PositionalParameters() throws Exception {
		QueryTranslatorImpl qt = compile( "from Animal a where a.bodyWeight = ?1" );
		AST ast = ( AST ) qt.getSqlAST();

		// make certain that the ejb3-positional param got recognized as a named param
		List namedParams = ASTUtil.collectChildren(
		        ast,
		        new ASTUtil.FilterPredicate() {
			        public boolean exclude(AST n) {
				        return n.getType() != HqlSqlTokenTypes.NAMED_PARAM;
			        }
		        }
		);
		assertTrue( "ejb3 positional param not recognized as a named param", namedParams.size() > 0 );
	}

	@Test
	public void testSelectObjectClause() throws Exception {
		//parse("select object(m) from Model m");
		assertEjbqlEqualsHql( "select object(m) from Model m", "from Model m" );
	}

	@Test
	public void testCollectionMemberDeclaration() throws Exception {
		String hql = "select o from Animal a inner join a.offspring o";
		String ejbql = "select object(o) from Animal a, in(a.offspring) o";
		//parse(hql);
		//parse(ejbql);
		assertEjbqlEqualsHql( ejbql, hql );
	}

	@Test
	public void testIsEmpty() throws Exception {
		//String hql = "from Animal a where not exists (from a.offspring)";
		String hql = "from Animal a where not exists elements(a.offspring)";
		String ejbql = "select object(a) from Animal a where a.offspring is empty";
		//parse(hql);
		//parse(ejbql);
		assertEjbqlEqualsHql(ejbql, hql);

		hql = "from Animal a where exists (from a.mother.father.offspring)";
		ejbql = "select object(a) from Animal a where a.mother.father.offspring is not empty";
		assertEjbqlEqualsHql( ejbql, hql );
	}

	@Test
	public void testMemberOf() throws Exception {
		String hql = "from Animal a where a.mother in (from a.offspring)";
		//String hql = "from Animal a where a.mother in elements(a.offspring)";
		String ejbql = "select object(a) from Animal a where a.mother member of a.offspring";
		//parse(hql);
		//parse(ejbql);
		assertEjbqlEqualsHql( ejbql, hql );

		hql = "from Animal a where a.mother not in (from a.offspring)";
		//hql = "from Animal a where a.mother not in elements(a.offspring)";
		ejbql = "select object(a) from Animal a where a.mother not member of a.offspring";
		//parse(hql);
		//parse(ejbql);
		assertEjbqlEqualsHql( ejbql, hql );
	}

	@Test
	public void testEJBQLFunctions() throws Exception {
		String hql = "select object(a) from Animal a where a.description = concat('1', concat('2','3'), '4'||'5')||0";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "from Animal a where substring(a.description, 1, 3) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select substring(a.description, 1, 3) from Animal a";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "from Animal a where lower(a.description) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select lower(a.description) from Animal a";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "from Animal a where upper(a.description) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select upper(a.description) from Animal a";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "from Animal a where length(a.description) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select length(a.description) from Animal a";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "from Animal a where locate(a.description, 'abc', 2) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select locate(a.description, :p1, 2) from Animal a";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select object(a) from Animal a where trim(trailing '_' from a.description) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select trim(trailing '_' from a.description) from Animal a";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select object(a) from Animal a where trim(leading '_' from a.description) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select object(a) from Animal a where trim(both a.description) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select object(a) from Animal a where trim(a.description) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select object(a) from Animal a where abs(a.bodyWeight) = sqrt(a.bodyWeight)";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select object(a) from Animal a where mod(a.bodyWeight, a.mother.bodyWeight) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select object(a) from Animal a where BIT_LENGTH(a.bodyWeight) = :p1";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select BIT_LENGTH(a.bodyWeight) from Animal a";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select object(a) from Animal a where CURRENT_DATE = :p1 or CURRENT_TIME = :p2 or CURRENT_TIMESTAMP = :p3";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		// todo the following is not supported
		//hql = "select CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP from Animal a";
		//parse(hql, true);
		//System.out.println("sql: " + toSql(hql));

		hql = "select object(a) from Animal a where a.bodyWeight like '%a%'";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select object(a) from Animal a where a.bodyWeight not like '%a%'";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );

		hql = "select object(a) from Animal a where a.bodyWeight like '%a%' escape '%'";
		parse( hql, false );
		System.out.println( "sql: " + toSql( hql ) );
	}

	@Test
	public void testTrueFalse() throws Exception {
		assertEjbqlEqualsHql( "from Human h where h.pregnant is true", "from Human h where h.pregnant = true" );
		assertEjbqlEqualsHql( "from Human h where h.pregnant is false", "from Human h where h.pregnant = false" );
		assertEjbqlEqualsHql( "from Human h where not(h.pregnant is true)", "from Human h where not( h.pregnant=true )" );
	}


	private void assertEjbqlEqualsHql(String ejbql, String hql) {
		QueryTranslatorFactory ast = new ASTQueryTranslatorFactory();

		QueryTranslator queryTranslator = ast.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, sessionFactory(), null );
		queryTranslator.compile( Collections.EMPTY_MAP, true );
		String hqlSql = queryTranslator.getSQLString();

		queryTranslator = ast.createQueryTranslator( ejbql, ejbql, Collections.EMPTY_MAP, sessionFactory(), null );
		queryTranslator.compile( Collections.EMPTY_MAP, true );
		String ejbqlSql = queryTranslator.getSQLString();

		assertEquals( hqlSql, ejbqlSql );
	}

	private QueryTranslatorImpl compile(String input) {
		QueryTranslatorFactory ast = new ASTQueryTranslatorFactory();
		QueryTranslator queryTranslator = ast.createQueryTranslator( input, input, Collections.EMPTY_MAP, sessionFactory(), null );
		queryTranslator.compile( Collections.EMPTY_MAP, true );

		return ( QueryTranslatorImpl ) queryTranslator;
	}

	private AST parse(String input, boolean logging) throws RecognitionException, TokenStreamException {
		if ( logging ) {
			System.out.println( "input: ->" + input + "<-" );
		}

		HqlParser parser = HqlParser.getInstance( input );
		parser.setFilter( false );
		parser.statement();
		AST ast = parser.getAST();

		if ( logging ) {
			System.out.println( "AST  :  " + ast.toStringTree() + "" );
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			parser.showAst( ast, new PrintStream( baos ) );
			System.out.println( baos.toString() );
		}

		assertEquals( "At least one error occurred during parsing!", 0, parser.getParseErrorHandler().getErrorCount() );

		return ast;
	}

	private String toSql(String hql) {
		QueryTranslatorFactory ast = new ASTQueryTranslatorFactory();
		QueryTranslator queryTranslator = ast.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, sessionFactory(), null );
		queryTranslator.compile( Collections.EMPTY_MAP, true );
		return queryTranslator.getSQLString();
	}
}
