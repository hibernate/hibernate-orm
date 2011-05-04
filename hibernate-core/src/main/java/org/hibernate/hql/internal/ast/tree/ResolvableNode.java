/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.hql.internal.ast.tree;
import antlr.SemanticException;
import antlr.collections.AST;

/**
 * The contract for expression sub-trees that can resolve themselves.
 *
 * @author josh
 */
public interface ResolvableNode {
	/**
	 * Does the work of resolving an identifier or a dot
	 */
	void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent) throws SemanticException;

	/**
	 * Does the work of resolving an identifier or a dot, but without a parent node
	 */
	void resolve(boolean generateJoin, boolean implicitJoin, String classAlias) throws SemanticException;

	/**
	 * Does the work of resolving an identifier or a dot, but without a parent node or alias
	 */
	void resolve(boolean generateJoin, boolean implicitJoin) throws SemanticException;

	/**
	 * Does the work of resolving inside of the scope of a function call
	 */
	void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin) throws SemanticException;

	/**
	 * Does the work of resolving an an index [].
	 */
	void resolveIndex(AST parent) throws SemanticException;

}
