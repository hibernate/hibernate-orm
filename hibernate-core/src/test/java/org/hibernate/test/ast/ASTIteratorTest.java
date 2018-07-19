/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ast;

import java.io.PrintWriter;

import antlr.ASTFactory;
import antlr.collections.AST;
import org.junit.Test;

import org.hibernate.hql.internal.antlr.HqlTokenTypes;
import org.hibernate.hql.internal.ast.HqlParser;
import org.hibernate.hql.internal.ast.util.ASTIterator;
import org.hibernate.hql.internal.ast.util.ASTParentsFirstIterator;
import org.hibernate.hql.internal.ast.util.ASTPrinter;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.hql.internal.ast.util.TokenPrinters;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test ASTIterator.
 */
public class ASTIteratorTest extends BaseUnitTestCase {
	private ASTFactory factory = new ASTFactory();

	@Test
	public void testSimpleTree() throws Exception {
		String input = "select foo from foo in class org.hibernate.test.Foo, fee in class org.hibernate.test.Fee where foo.dependent = fee order by foo.string desc, foo.component.count asc, fee.id";
		HqlParser parser = HqlParser.getInstance( input );
		parser.statement();
		AST ast = parser.getAST();
		ASTPrinter printer = TokenPrinters.HQL_TOKEN_PRINTER;
		printer.showAst( ast, new PrintWriter( System.out ) );
		ASTIterator iterator = new ASTIterator( ast );
		int count = 0;
		while ( iterator.hasNext() ) {
			assertTrue( iterator.next() instanceof AST );
			count++;
		}
		assertEquals( 43, count );

		UnsupportedOperationException uoe = null;
		try {
			iterator.remove();
		}
		catch ( UnsupportedOperationException e ) {
			uoe = e;
		}
		assertNotNull( uoe );
	}

	@Test
	public void testParentsFirstIterator() throws Exception {
		AST[] tree = new AST[4];
		AST grandparent = tree[0] = ASTUtil.create( factory, 1, "grandparent" );
		AST parent = tree[1] = ASTUtil.create( factory, 2, "parent" );
		AST child = tree[2] = ASTUtil.create( factory, 3, "child" );
		AST baby = tree[3] = ASTUtil.create( factory, 4, "baby" );
		AST t = ASTUtil.createTree( factory, tree );
		AST brother = ASTUtil.create( factory, 10, "brother" );
		child.setNextSibling( brother );
		AST sister = ASTUtil.create( factory, 11, "sister" );
		brother.setNextSibling( sister );
		AST uncle = factory.make( new AST[]{
			factory.create( 20, "uncle" ),
			factory.create( 21, "cousin1" ),
			factory.create( 22, "cousin2" ),
			factory.create( 23, "cousin3" )} );
		parent.setNextSibling( uncle );
		System.out.println( t.toStringTree() );

		System.out.println( "--- ASTParentsFirstIterator ---" );
		ASTParentsFirstIterator iter = new ASTParentsFirstIterator( t );
		int count = 0;
		while ( iter.hasNext() ) {
			AST n = iter.nextNode();
			count++;
			System.out.println( n );
		}
		assertEquals( 10, count );

		System.out.println( "--- ASTIterator ---" );
		ASTIterator iter2 = new ASTIterator( t );
		int count2 = 0;
		while ( iter2.hasNext() ) {
			AST n = iter2.nextNode();
			count2++;
			System.out.println( n );
		}
		assertEquals( 10, count2 );

		System.out.println( "--- ASTParentsFirstIterator (parent) ---" );
		ASTParentsFirstIterator iter3 = new ASTParentsFirstIterator( parent );
		int count3 = 0;
		while ( iter3.hasNext() ) {
			AST n = iter3.nextNode();
			count3++;
			System.out.println( n );
		}
		assertEquals( 5, count3 );
	}
}
