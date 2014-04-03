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
package org.hibernate.hql.internal.ast;

import java.lang.reflect.Constructor;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.tree.AggregateNode;
import org.hibernate.hql.internal.ast.tree.BetweenOperatorNode;
import org.hibernate.hql.internal.ast.tree.BinaryArithmeticOperatorNode;
import org.hibernate.hql.internal.ast.tree.BinaryLogicOperatorNode;
import org.hibernate.hql.internal.ast.tree.BooleanLiteralNode;
import org.hibernate.hql.internal.ast.tree.CastFunctionNode;
import org.hibernate.hql.internal.ast.tree.SearchedCaseNode;
import org.hibernate.hql.internal.ast.tree.SimpleCaseNode;
import org.hibernate.hql.internal.ast.tree.CollectionFunction;
import org.hibernate.hql.internal.ast.tree.ConstructorNode;
import org.hibernate.hql.internal.ast.tree.CountNode;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.DotNode;
import org.hibernate.hql.internal.ast.tree.FromClause;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.IdentNode;
import org.hibernate.hql.internal.ast.tree.ImpliedFromElement;
import org.hibernate.hql.internal.ast.tree.InLogicOperatorNode;
import org.hibernate.hql.internal.ast.tree.IndexNode;
import org.hibernate.hql.internal.ast.tree.InitializeableNode;
import org.hibernate.hql.internal.ast.tree.InsertStatement;
import org.hibernate.hql.internal.ast.tree.IntoClause;
import org.hibernate.hql.internal.ast.tree.IsNotNullLogicOperatorNode;
import org.hibernate.hql.internal.ast.tree.IsNullLogicOperatorNode;
import org.hibernate.hql.internal.ast.tree.JavaConstantNode;
import org.hibernate.hql.internal.ast.tree.LiteralNode;
import org.hibernate.hql.internal.ast.tree.MapEntryNode;
import org.hibernate.hql.internal.ast.tree.MapKeyNode;
import org.hibernate.hql.internal.ast.tree.MapValueNode;
import org.hibernate.hql.internal.ast.tree.MethodNode;
import org.hibernate.hql.internal.ast.tree.OrderByClause;
import org.hibernate.hql.internal.ast.tree.ParameterNode;
import org.hibernate.hql.internal.ast.tree.QueryNode;
import org.hibernate.hql.internal.ast.tree.ResultVariableRefNode;
import org.hibernate.hql.internal.ast.tree.SelectClause;
import org.hibernate.hql.internal.ast.tree.SelectExpressionImpl;
import org.hibernate.hql.internal.ast.tree.SessionFactoryAwareNode;
import org.hibernate.hql.internal.ast.tree.SqlFragment;
import org.hibernate.hql.internal.ast.tree.SqlNode;
import org.hibernate.hql.internal.ast.tree.UnaryArithmeticNode;
import org.hibernate.hql.internal.ast.tree.UnaryLogicOperatorNode;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;

import antlr.ASTFactory;
import antlr.Token;
import antlr.collections.AST;

/**
 * Custom AST factory the intermediate tree that causes ANTLR to create specialized
 * AST nodes, given the AST node type (from HqlSqlTokenTypes).   HqlSqlWalker registers
 * this factory with itself when it is initialized.
 *
 * @author Joshua
 */
public class SqlASTFactory extends ASTFactory implements HqlSqlTokenTypes {
	private HqlSqlWalker walker;

	/**
	 * Create factory with a specific mapping from token type
	 * to Java AST node type.  Your subclasses of ASTFactory
	 * can override and reuse the map stuff.
	 */
	public SqlASTFactory(HqlSqlWalker walker) {
		super();
		this.walker = walker;
	}

	/**
	 * Returns the class for a given token type (a.k.a. AST node type).
	 *
	 * @param tokenType The token type.
	 *
	 * @return Class - The AST node class to instantiate.
	 */
	public Class getASTNodeType(int tokenType) {
		switch ( tokenType ) {
			case SELECT:
			case QUERY:
				return QueryNode.class;
			case UPDATE:
				return UpdateStatement.class;
			case DELETE:
				return DeleteStatement.class;
			case INSERT:
				return InsertStatement.class;
			case INTO:
				return IntoClause.class;
			case FROM:
				return FromClause.class;
			case FROM_FRAGMENT:
				return FromElement.class;
			case IMPLIED_FROM:
				return ImpliedFromElement.class;
			case DOT:
				return DotNode.class;
			case INDEX_OP:
				return IndexNode.class;
			// Alias references and identifiers use the same node class.
			case ALIAS_REF:
			case IDENT:
				return IdentNode.class;
			case RESULT_VARIABLE_REF:
				return ResultVariableRefNode.class;
			case SQL_TOKEN:
				return SqlFragment.class;
			case METHOD_CALL:
				return MethodNode.class;
			case CAST:
				return CastFunctionNode.class;
			case ELEMENTS:
			case INDICES:
				return CollectionFunction.class;
			case SELECT_CLAUSE:
				return SelectClause.class;
			case SELECT_EXPR:
				return SelectExpressionImpl.class;
			case AGGREGATE:
				return AggregateNode.class;
			case COUNT:
				return CountNode.class;
			case CONSTRUCTOR:
				return ConstructorNode.class;
			case NUM_INT:
			case NUM_FLOAT:
			case NUM_LONG:
			case NUM_DOUBLE:
			case NUM_BIG_INTEGER:
			case NUM_BIG_DECIMAL:
			case QUOTED_STRING:
				return LiteralNode.class;
			case TRUE:
			case FALSE:
				return BooleanLiteralNode.class;
			case JAVA_CONSTANT:
				return JavaConstantNode.class;
			case ORDER:
				return OrderByClause.class;
			case PLUS:
			case MINUS:
			case STAR:
			case DIV:
			case MOD:
				return BinaryArithmeticOperatorNode.class;
			case UNARY_MINUS:
			case UNARY_PLUS:
				return UnaryArithmeticNode.class;
			case CASE2:
				return SimpleCaseNode.class;
			case CASE:
				return SearchedCaseNode.class;
			case PARAM:
			case NAMED_PARAM:
				return ParameterNode.class;
			case EQ:
			case NE:
			case LT:
			case GT:
			case LE:
			case GE:
			case LIKE:
			case NOT_LIKE:
				return BinaryLogicOperatorNode.class;
			case IN:
			case NOT_IN:
				return InLogicOperatorNode.class;
			case BETWEEN:
			case NOT_BETWEEN:
				return BetweenOperatorNode.class;
			case IS_NULL:
				return IsNullLogicOperatorNode.class;
			case IS_NOT_NULL:
				return IsNotNullLogicOperatorNode.class;
			case EXISTS:
				return UnaryLogicOperatorNode.class;
			case KEY: {
				return MapKeyNode.class;
			}
			case VALUE: {
				return MapValueNode.class;
			}
			case ENTRY: {
				return MapEntryNode.class;
			}
			default:
				return SqlNode.class;
		}
	}

	@SuppressWarnings("unchecked")
	protected AST createUsingCtor(Token token, String className) {
		Class c;
		AST t;
		try {
			c = Class.forName( className );
			Class[] tokenArgType = new Class[] {antlr.Token.class};
			Constructor ctor = c.getConstructor( tokenArgType );
			if ( ctor != null ) {
				t = (AST) ctor.newInstance( token );
				initializeSqlNode( t );
			}
			else {
				// just do the regular thing if you can't find the ctor
				// Your AST must have default ctor to use this.
				t = create( c );
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException( "Invalid class or can't make instance, " + className );
		}
		return t;
	}

	private void initializeSqlNode(AST t) {
		// Initialize SQL nodes here.
		if ( t instanceof InitializeableNode ) {
			InitializeableNode initializeableNode = (InitializeableNode) t;
			initializeableNode.initialize( walker );
		}
		if ( t instanceof SessionFactoryAwareNode ) {
			( (SessionFactoryAwareNode) t ).setSessionFactory( walker.getSessionFactoryHelper().getFactory() );
		}
	}

	/**
	 * Actually instantiate the AST node.
	 *
	 * @param c The class to instantiate.
	 *
	 * @return The instantiated and initialized node.
	 */
	protected AST create(Class c) {
		AST t;
		try {
			t = (AST) c.newInstance();
			initializeSqlNode( t );
		}
		catch (Exception e) {
			error( "Can't create AST Node " + c.getName() );
			return null;
		}
		return t;
	}

}
