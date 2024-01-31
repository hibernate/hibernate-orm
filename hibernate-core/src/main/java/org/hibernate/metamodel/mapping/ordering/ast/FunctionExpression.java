/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.internal.AbstractDomainPath;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.SortDirection;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SortSpecification;

/**
 * Represents a function used in an order-by fragment
 *
 * @author Steve Ebersole
 */
public class FunctionExpression implements OrderingExpression, FunctionRenderer {
	private final String name;
	private final List<OrderingExpression> arguments;

	public FunctionExpression(String name, int numberOfArguments) {
		this.name = name;
		this.arguments = numberOfArguments == 0
				? Collections.emptyList()
				: new ArrayList<>( numberOfArguments );
	}

	public String getName() {
		return name;
	}

	public List<OrderingExpression> getArguments() {
		return arguments;
	}

	public void addArgument(OrderingExpression argument) {
		arguments.add( argument );
	}

	@Override
	public SelfRenderingFunctionSqlAstExpression resolve(
			QuerySpec ast,
			TableGroup tableGroup,
			String modelPartName,
			SqlAstCreationState creationState) {

		final int size = arguments.size();
		final List<SqlAstNode> args = new ArrayList<>( size );
		for ( int i = 0; i < size; i++ ) {
			final OrderingExpression orderingExpression = arguments.get( i );
			final String subModelPartName;
			if ( orderingExpression instanceof DomainPath ) {
				final String partName = ( (DomainPath) orderingExpression ).getNavigablePath().getLocalName();
				if ( CollectionPart.Nature.ELEMENT.getName().equals( partName ) ) {
					subModelPartName = AbstractDomainPath.ELEMENT_TOKEN;
				}
				else {
					subModelPartName = partName;
				}
			}
			else {
				subModelPartName = null;
			}
			args.add( orderingExpression.resolve( ast, tableGroup, subModelPartName, creationState ) );
		}

		return new SelfRenderingFunctionSqlAstExpression(
				name,
				this,
				args,
				null,
				tableGroup.getModelPart().findSubPart( modelPartName, null )
		);
	}

	@Override
	public void apply(
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence,
			SqlAstCreationState creationState) {
		final SelfRenderingFunctionSqlAstExpression expression = resolve( ast, tableGroup, modelPartName, creationState );
		final Expression sortExpression = OrderingExpression.applyCollation(
				expression,
				collation,
				creationState
		);
		ast.addSortSpecification( new SortSpecification( sortExpression, sortOrder, nullPrecedence ) );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( name );
		sqlAppender.appendSql( '(' );
		if ( !sqlAstArguments.isEmpty() ) {
			sqlAstArguments.get( 0 ).accept( walker );
			for ( int i = 1; i < sqlAstArguments.size(); i++ ) {
				sqlAppender.appendSql( ", " );
				sqlAstArguments.get( i ).accept( walker );
			}
		}
		sqlAppender.appendSql( ')' );
	}

	@Override
	public String toDescriptiveText() {
		return "function (" + name + ")";
	}
}
