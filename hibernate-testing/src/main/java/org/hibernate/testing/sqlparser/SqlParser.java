/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.sqlparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Parses <strong><em>valid</em></strong> SQL DDL and DML (not DCL or TCL), including calls to ODBC functions
 */
public class SqlParser {

	private transient StringTokenizer tokenizer;
	private transient String token;
	private transient String nextToken;
	private Map< String, Table > tablesByName = new HashMap< String, Table >();

	private void appendNext() {
		String oldToken = token;
		token = oldToken + ' ' + next();
	}

	private void appendNextIfSuffix() {
		if ( nextToken == null ) {
			return;
		}
		if ( token.startsWith( "'" ) && nextToken.startsWith( "'" )
				|| ( token.endsWith( "." ) && ( quoted( nextToken ) || nextMatches( "*" ) ) ) ) {
			token += nextToken;
			nextToken = next_();
			appendNextIfSuffix();
		}
	}

	public void clear() {
		tablesByName.clear();
	}

	private boolean endOfExpression() {
		return matches(
				";",
				"AS",
				"FROM",
				"WHERE",
				"LEFT",
				"INNER",
				"OUTER",
				"CROSS",
				"GROUP",
				"HAVING",
				"ORDER",
				")",
				",",
				"FOR",
				"WHEN",
				"THEN",
				"ELSE",
				"END",
				"LIMIT",
				"OFFSET",
				"UNION" );
	}

	private boolean match( String token, String... matchingTokens ) {
		for ( String matchingToken : matchingTokens ) {
			if ( matchingToken.equalsIgnoreCase( token ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean matches( String... tokens ) {
		return match( token, tokens );
	}

	private String next() {
		token = token == null ? next_() : nextToken;
		nextToken = ";".equals( nextToken ) ? null : next_();
		appendNextIfSuffix();
		return token;
	}

	private String next_() {
		String token = tokenizer.nextToken().trim();
		while ( token.isEmpty() && tokenizer.hasMoreTokens() ) {
			token = tokenizer.nextToken().trim();
		}
		if ( quoted( token ) ) {
			String quote = token;
			StringBuilder builder = new StringBuilder( token );
			do {
				token = tokenizer.nextToken();
				builder.append( token );
			} while ( !quote.equals( token ) );
			return builder.toString();
		}
		return token;
	}

	private void nextAndMatches( String... tokens ) {
		for ( String token : tokens ) {
			next();
			if ( !matches( token ) ) {
				throw new UnexpectedTokenException( "token (Expected " + nextToken + ")", token );
			}
		}
	}

	private boolean nextMatches( String... tokens ) {
		return match( nextToken, tokens );
	}

	/**
	 * @param sql
	 * @return the object model of the supplied SQL
	 * @throws UnexpectedTokenException if an unexpected token is encountered in the supplied SQL
	 */
	public Statement parse( String sql ) {
		if ( !sql.endsWith( ";" ) ) {
			sql += ";";
		}
		token = nextToken = null;
		tokenizer = new StringTokenizer( sql, " \"'();,*/+-=<>&|^%!~:{}", true );
		next();
		if ( matches( "CREATE" ) ) {
			return parseCreate();
		}
		if ( matches( "ALTER" ) ) {
			return parseAlter();
		}
		if ( matches( "SELECT" ) ) {
			return parseSelect();
		}
		if ( matches( "CALL" ) ) {
			return parseCall();
		}
		if ( matches( "INSERT" ) ) {
			return parseInsert();
		}
		if ( matches( "DELETE" ) ) {
			return parseDelete();
		}
		if ( matches( "UPDATE" ) ) {
			return parseUpdate();
		}
		throw new UnexpectedTokenException( "statement", token );
	}

	private Aliasable parseAliasable() {
		Object expr;
		if ( matches( "(" ) ) {
			expr = parseExpression();
		} else {
			expr = new Name( token );
			next();
		}
		return parseAliasable( expr );
	}

	private Alias parseAlias() {
		Alias alias = new Alias();
		alias.name = next();
		nextAndMatches( "AS" );
		alias.sourceCode = next();
		for ( next(); !matches( ";" ); next() ) {
			alias.sourceCode = token;
		}
		return alias;
	}

	private Aliasable parseAliasable( Object expression ) {
		Aliasable aliasable = new Aliasable();
		aliasable.name = expression;
		if ( matches( "AS" ) ) {
			aliasable.alias = next();
		} else if ( endOfExpression() ) {
			return aliasable;
		} else {
			aliasable.alias = token;
		}
		next();
		return aliasable;
	}

	private Statement parseAlter() {
		next();
		if ( matches( "TABLE" ) ) {
			Table table = tablesByName.get( new Name( next() ).unquoted() );
			nextAndMatches( "ADD", "CONSTRAINT" );
			Name name = new Name( next() );
			next();
			if ( matches( "FOREIGN" ) ) {
				ForeignKey key = new ForeignKey();
				table.constraints.add( key );
				key.name = name;
				parseKey( key.columns );
				nextAndMatches( "REFERENCES" );
				key.references = new Name( next() );
			} else if ( matches( "UNIQUE" ) ) {
				Unique constraint = new Unique();
				table.constraints.add( constraint );
				constraint.name = name;
				next();
				parseColumns( constraint.columns );
			}
			return null;
		}
		throw new UnexpectedTokenException( "alter type", token );
	}

	private Call parseCall() {
		Call call = new Call();
		next();
		if ( matches( "NEXT" ) ) {
			nextAndMatches( "VALUE", "FOR" );
			NextValueFor function = new NextValueFor();
			call.function = function;
			function.operands.add( new Name( next() ) );
		} else if ( nextMatches( "(" ) ) {
			call.function = parseFunction();
		} else {
			throw new UnexpectedTokenException( "call procedure", token );
		}
		return call;
	}

	private Check parseCheck() {
		Check check = new Check();
		nextAndMatches( "(" );
		next();
		check.operands.add( parseExpression() );
		next();
		return check;
	}

	private void parseColumns( List< Name > columns ) {
		do {
			columns.add( new Name( next() ) );
			next();
		} while ( matches( "," ) );
	}

	private Statement parseCreate() {
		next();
		if ( matches( "SEQUENCE" ) ) {
			return parseSequence();
		}
		if ( matches( "TABLE", "CACHED", "LOCAL", "GLOBAL", "TEMPORARY" ) ) {
			return parseTable();
		}
		if ( matches( "INDEX" ) ) {
			return parseIndex();
		}
		if ( matches( "ALIAS" ) ) {
			return parseAlias();
		}
		throw new UnexpectedTokenException( "create type", token );
	}

	private Delete parseDelete() {
		Delete delete = new Delete();
		nextAndMatches( "FROM" );
		delete.table = new Name( next() );
		next();
		if ( matches( "WHERE" ) ) {
			next();
			delete.where = parseExpression();
		}
		return delete;
	}

	private Object parseExpression() {
		Expression expression = new Expression();
		if ( matches( "SELECT" ) ) {
			return parseSelect();
		}
		parseOperand( expression );
		return parseOperator( expression );
	}

	private Function parseFunction() {
		Function function;
		if ( matches( "EXTRACT" ) ) {
			function = new Extract();
		} else if ( matches( "COUNT" ) ) {
			function = new Count();
		} else if ( matches( "CAST" ) ) {
			function = new Cast();
		} else if ( matches( "TRIM" ) ) {
			function = new Trim();
		} else {
			function = new Function();
		}
		function.operators.add( token );
		nextAndMatches( "(" );
		next();
		while ( !matches( ")" ) ) {
			if ( matches( "DISTINCT" ) ) {
				( ( Count ) function ).distinct = true;
				next();
			} else if ( matches( "LEADING", "TRAILING", "BOTH" ) ) {
				( ( Trim ) function ).type = token;
				next();
			}
			if ( matches( ",", "FROM", "AS" ) ) {
				next();
			}
			function.operands.add( parseExpression() );
		}
		next();
		return function;
	}

	private Index parseIndex() {
		Index index = new Index();
		index.name = new Name( next() );
		nextAndMatches( "ON" );
		index.table = new Name( next() );
		nextAndMatches( "(" );
		do {
			index.columns.add( new Name( next() ) );
			next();
		} while ( matches( "," ) );
		next();
		return index;
	}

	private Insert parseInsert() {
		Insert insert = new Insert();
		nextAndMatches( "INTO" );
		insert.table = new Name( next() );
		next();
		if ( matches( "SELECT" ) ) {
			insert.select = parseSelect();
		} else {
			if ( matches( "(" ) ) {
				parseColumns( insert.columns );
				next();
			}
			if ( matches( "SELECT" ) ) {
				insert.select = parseSelect();
			} else if ( matches( "VALUES" ) ) {
				nextAndMatches( "(" );
				next();
				while ( !matches( ")" ) ) {
					if ( matches( "," ) ) {
						next();
					}
					insert.values.add( parseExpression() );
				}
				next();
			}
		}
		if ( matches( "WHERE" ) ) {
			next();
			insert.where = parseExpression();
		}
		return insert;
	}

	private void parseKey( List< Name > columns ) {
		nextAndMatches( "KEY" );
		next();
		parseColumns( columns );
	}

	private void parseOperand( Expression expression ) {
		if ( matches( "CASE" ) ) {
			Case caseExpr = new Case();
			next();
			if ( !matches( "WHEN" ) ) {
				caseExpr.expression = parseExpression();
				next();
			}
			while ( matches( "WHEN" ) ) {
				When when = new When();
				caseExpr.operands.add( when );
				next();
				when.condition = parseExpression();
				next();
				when.then = parseExpression();
				if ( matches( "ELSE" ) ) {
					next();
					when.elseExpression = parseExpression();
				}
			}
			expression.operands.add( caseExpr );
			next();
		} else if ( matches( "(" ) ) {
			if ( !expression.operators.isEmpty() && match( expression.operators.get( 0 ), "IN", "NOT IN" ) ) {
				List< Object > exprs = new ArrayList< Object >();
				next();
				while ( !matches( ")" ) ) {
					if ( matches( "," ) ) {
						next();
					}
					exprs.add( parseExpression() );
				}
				expression.operands.add( exprs );
			} else {
				next();
				expression.operands.add( parseExpression() );
			}
			next();
		} else if ( matches( "NOT", "EXISTS", "ANY", "ALL", "SOME", "!", "~", "-" ) ) {
			if ( nextMatches( "EXISTS" ) ) {
				appendNext();
			}
			if ( matches( "EXISTS", "NOT EXISTS", "ANY", "ALL", "SOME" ) ) {
				expression.operators.add( token );
				nextAndMatches( "(" );
				expression.operands.add( parseExpression() );
			}
		} else if ( matches( "{" ) ) {
			nextAndMatches( "fn" );
			OdbcFunction fn = new OdbcFunction();
			expression.operands.add( fn );
			next();
			fn.function = parseFunction();
			next();
		} else if ( nextMatches( "(" ) ) {
			expression.operands.add( parseFunction() );
		} else {
			expression.operands.add( new Name( token ) );
			next();
		}
	}

	private Object parseOperator( Expression expression ) {
		if ( endOfExpression() ) {
			return expression;
		}
		int precedence = precedence( expression );
		if ( precedence < 0 ) {
			return expression;
		}
		expression.operators.add( token );
		next();
		parseOperand( expression );
		if ( endOfExpression() ) {
			return expression;
		}
		if ( match( expression.operators.get( 0 ), "BETWEEN" ) ) {
			expression.operators.add( token );
			next();
			if ( matches( "(" ) ) {
				expression.operands.add( next() );
				nextAndMatches( ")" );
			} else {
				expression.operands.add( token );
			}
			next();
			if ( endOfExpression() ) {
				return expression;
			}
		}
		if ( precedence < precedence( expression ) ) {
			Expression expr = new Expression();
			expr.operands.add( expression.operands.remove( expression.operands.size() - 1 ) );
			expression.operands.add( expr );
			expr.operators.add( token );
			next();
			parseOperand( expr );
			if ( endOfExpression() ) {
				return expression;
			}
		}
		Expression expr = expression;
		expression = new Expression();
		expression.operands.add( expr );
		return parseOperator( expression );
	}

	private Select parseSelect() {
		Select select = new Select();
		if ( nextMatches( "DISTINCT" ) ) {
			select.distinct = true;
			next();
		}
		while ( !matches( "FROM", ";" ) ) {
			next();
			select.columns.add( parseAliasable( parseExpression() ) );
		}
		if ( matches( "FROM" ) ) {
			do {
				next();
				From from = new From();
				select.froms.add( from );
				from.aliasable = parseAliasable();
				while ( matches( "LEFT", "OUTER", "INNER", "CROSS" ) ) {
					Join join = new Join();
					from.joins.add( join );
					if ( matches( "LEFT" ) ) {
						join.left = true;
						next();
					}
					join.type = token;
					nextAndMatches( "JOIN" );
					next();
					join.table = parseAliasable();
					if ( matches( "ON" ) ) {
						next();
						join.on = parseExpression();
					}
				}
			} while ( matches( "," ) );
		}
		if ( matches( "WHERE" ) ) {
			next();
			select.where = parseExpression();
		}
		if ( matches( "GROUP" ) ) {
			nextAndMatches( "BY" );
			select.groupBy = next();
			next();
		}
		if ( matches( "HAVING" ) ) {
			next();
			select.having = parseExpression();
		}
		if ( matches( "ORDER" ) ) {
			nextAndMatches( "BY" );
			do {
				OrderBy orderBy = new OrderBy();
				select.orderBy.add( orderBy );
				orderBy.column = next();
				next();
				if ( matches( "ASC" ) ) {
					next();
				} else if ( matches( "DESC" ) ) {
					orderBy.descending = true;
					next();
				}
			} while ( matches( "," ) );
		}
		if ( matches( "LIMIT" ) ) {
			next();
			select.limit = parseExpression();
		}
		if ( matches( "OFFSET" ) ) {
			next();
			select.offset = parseExpression();
		}
		if ( matches( "FOR" ) ) {
			nextAndMatches( "UPDATE" );
			select.forUpdate = true;
			next();
		}
		if ( matches( "UNION" ) ) {
			next();
			if ( matches( "ALL" ) ) {
				select.unionAll = true;
				next();
			}
			select.union = parseSelect();
		}
		return select;
	}

	private Sequence parseSequence() {
		Sequence sequence = new Sequence();
		sequence.name = new Name( next() );
		next();
		if ( matches( "START" ) ) {
			nextAndMatches( "WITH" );
			sequence.start = Integer.parseInt( next() );
			next();
		}
		if ( matches( "INCREMENT" ) ) {
			nextAndMatches( "BY" );
			sequence.increment = Integer.parseInt( next() );
		}
		return sequence;
	}

	private Table parseTable() {
		Table table = new Table();
		if ( matches( "CACHED" ) ) {
			table.cached = true;
			next();
		}
		if ( matches( "LOCAL", "GLOBAL" ) ) {
			table.temporaryType = token;
			nextAndMatches( "TEMPORARY" );
		}
		if ( matches( "TEMPORARY" ) ) {
			table.temporary = true;
			nextAndMatches( "TABLE" );
		}
		if ( matches( "TABLE" ) ) {
			next();
		}
		if ( matches( "IF" ) ) {
			table.ifNotExists = true;
			nextAndMatches( "NOT", "EXISTS" );
			next();
		}
		table.name = new Name( token );
		tablesByName.put( table.name.unquoted(), table );
		for ( nextAndMatches( "(" ); !matches( ")" ); ) {
			next();
			if ( matches( "PRIMARY" ) ) {
				PrimaryKey key = new PrimaryKey();
				table.constraints.add( key );
				parseKey( key.columns );
				next();
			} else if ( matches( "CHECK" ) ) {
				table.constraints.add( parseCheck() );
			} else {
				Column column = new Column();
				table.columns.add( column );
				column.name = new Name( token );
				column.type = next();
				next();
				if ( matches( "(" ) ) {
					column.length = Integer.parseInt( next() );
					next();
					if ( matches( "," ) ) {
						column.decimals = Integer.parseInt( next() );
						nextAndMatches( ")" );
					}
					next();
				}
				if ( matches( "NOT" ) ) {
					nextAndMatches( "NULL" );
					column.notNull = true;
					next();
				}
				if ( matches( "CHECK" ) ) {
					column.check = parseCheck();
				}
				if ( matches( "GENERATED" ) ) {
					nextAndMatches( "BY", "DEFAULT", "AS", "IDENTITY" );
					column.generatedByDefaultAsIdentity = true;
					next();
				}
			}
		}
		next();
		if ( matches( "ON" ) ) {
			nextAndMatches( "COMMIT" );
			table.onCommit = next();
			for ( next(); !matches( ";" ); next() ) {
				table.onCommit += ' ' + token;
			}
		}
		return table;
	}

	private Update parseUpdate() {
		Update update = new Update();
		update.table = next();
		nextAndMatches( "SET" );
		do {
			next();
			update.sets.add( parseExpression() );
		} while ( matches( "," ) );
		if ( matches( "WHERE" ) ) {
			next();
			update.where = parseExpression();
		}
		return update;
	}

	private int precedence( Expression expression ) {
		if ( matches( "NOT" ) && nextMatches( "LIKE", "IN", "BETWEEN" ) ) {
			appendNext();
		} else {
			while ( nextMatches( "=", "<", ">", "|", "&" ) ) {
				String oldToken = token;
				token = oldToken + next();
			}
		}
		int precedence = 0;
		if ( matches( ":=" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "OR", "||" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "XOR" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "AND", "&&" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "NOT" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "BETWEEN", "NOT BETWEEN", "CASE" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches(
				"=",
				"<",
				">",
				"<=",
				">=",
				"!=",
				"<>",
				"<=>",
				"IN",
				"NOT IN",
				"LIKE",
				"NOT LIKE",
				"ESCAPE",
				"IS",
				"EXISTS",
				"NOT EXISTS",
				"ANY",
				"ALL",
				"SOME" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "|" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "&" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "<<", ">>" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "+" ) || ( matches( "-" ) && !expression.operands.isEmpty() ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "*", "/", "%", "MOD", "DIV" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "^" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "-", "~" ) ) {
			return precedence;
		}
		precedence++;
		if ( matches( "!" ) ) {
			return precedence;
		}
		return -1;
	}

	private boolean quoted( String token ) {
		return token.startsWith( "'" ) || token.startsWith( "\"" );
	}
}
