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
package org.hibernate.test.ast;

import antlr.ASTFactory;
import antlr.collections.AST;
import org.junit.Test;

import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Unit test for ASTUtil.
 */
public class ASTUtilTest extends BaseUnitTestCase {
	private ASTFactory factory = new ASTFactory();

	@Test
	public void testCreate() throws Exception {
		AST n = ASTUtil.create( factory, 1, "one");
		assertNull( n.getFirstChild() );
		assertEquals( "one", n.getText() );
		assertEquals( 1, n.getType() );
	}

	@Test
	public void testCreateTree() throws Exception {
		AST[] tree = new AST[4];
		AST grandparent = tree[0] = ASTUtil.create(factory, 1, "grandparent");
		AST parent = tree[1] = ASTUtil.create(factory,2,"parent");
		AST child = tree[2] = ASTUtil.create(factory,3,"child");
		AST baby = tree[3] = ASTUtil.create(factory,4,"baby");
		AST t = ASTUtil.createTree( factory, tree);
		assertSame(t,grandparent);
		assertSame(parent,t.getFirstChild());
		assertSame(child,t.getFirstChild().getFirstChild());
		assertSame( baby, t.getFirstChild().getFirstChild().getFirstChild() );
	}

	@Test
	public void testFindPreviousSibling() throws Exception {
		AST child1 = ASTUtil.create(factory,2, "child1");
		AST child2 = ASTUtil.create(factory,3, "child2");
		AST n = factory.make( new AST[] {
			ASTUtil.create(factory, 1, "parent"),
			child1,
			child2,
		});
		assertSame(child1,ASTUtil.findPreviousSibling( n,child2));
		Exception e = null;
		try {
			ASTUtil.findPreviousSibling(child1,null);
		}
		catch (Exception x) {
			e = x;
		}
		assertNotNull(e);
	}
}
