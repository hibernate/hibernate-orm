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
package org.hibernate.testing.sql;

import java.lang.reflect.Field;
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
	Map< String, NamedObject > globalObjectsByName = new HashMap< String, NamedObject >();

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
		globalObjectsByName.clear();
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

	private void mapGlobalObjectByName( NamedObject object ) {
		String unquotedName = object.name().unquoted().toLowerCase();
		globalObjectsByName.put( object instanceof Column
				? ( ( CreateTable ) ( ( SqlObject ) object ).parent() ).name().unquoted().toLowerCase() + '.' + unquotedName
				: unquotedName, object );
		object.localScope().mapLocalObjectByName( unquotedName, object );
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

	private Reference newGlobalReference( SqlObject parent, String text ) {
		return setGlobalReferent( new Reference( parent, text ) );
	}

	private Reference newGlobalReference( SqlObject parent, Reference tableReference, String text ) {
		Reference ref = new Reference( parent, text );
		text = ref.unquoted();
		if ( text.indexOf( '.' ) < 0 ) {
			text = ( ( CreateTable ) tableReference.referent ).name().unquoted() + '.' + text;
		}
		return setGlobalReferent( ref, text );
	}

	private String next() {
		token = token == null ? next_() : nextToken;
		nextToken = ";".equals( nextToken ) ? null : next_();
		appendNextIfSuffix();
		while ( matches( "<", ">", "=", "!", "&", "|", ":" ) && nextMatches( "=", "<", ">", "|", "&" ) ) {
			String oldToken = token;
			token = oldToken + next();
		}
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

	private Operation operation( SqlObject parent, boolean unary ) {
		int precedence = 0;
		if ( matches( ":=" ) ) {
			return new BinaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "OR", "||" ) ) {
			return new CommutativeBinaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "XOR" ) ) {
			return new CommutativeBinaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "AND", "&&" ) ) {
			return new CommutativeBinaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "NOT" ) ) {
			return new UnaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "BETWEEN" ) ) {
			return new Between( parent, token, precedence );
		}
		if ( matches( "CASE" ) ) {
			return new Case( parent, token, precedence );
		}
		precedence++;
		if ( matches( "=", "!=", "<>", "<=>", "LIKE", "IS" ) ) {
			return new CommutativeBinaryOperation( parent, token, precedence );
		}
		if ( matches( "<", ">", "<=", ">=", "IN", "ESCAPE" ) ) {
			return new BinaryOperation( parent, token, precedence );
		}
		if ( matches( "EXISTS", "ANY", "ALL", "SOME" ) ) {
			return new UnaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "|" ) ) {
			return new CommutativeBinaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "&" ) ) {
			return new CommutativeBinaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "<<", ">>" ) ) {
			return new BinaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "+" ) ) {
			return new CommutativeBinaryOperation( parent, token, precedence );
		}
		if ( !unary && matches( "-" ) ) {
			return new BinaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "*" ) ) {
			return new CommutativeBinaryOperation( parent, token, precedence );
		}
		if ( matches( "/", "%", "MOD", "DIV" ) ) {
			return new BinaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "^" ) ) {
			return new CommutativeBinaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "~" ) ) {
			return new UnaryOperation( parent, nextToken, precedence );
		}
		if ( unary && matches( "-", "~" ) ) {
			return new UnaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "!" ) ) {
			return new UnaryOperation( parent, token, precedence );
		}
		precedence++;
		if ( matches( "(" ) ) {
			return new Parentheses( parent, token, precedence );
		}
		return null;
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
		Statement statement;
		if ( matches( "CREATE" ) ) {
			statement = parseCreate();
		} else if ( matches( "ALTER" ) ) {
			statement = parseAlterTable();
		} else if ( matches( "SELECT" ) ) {
			statement = parseSelect( null );
		} else if ( matches( "CALL" ) ) {
			statement = parseCall();
		} else if ( matches( "INSERT" ) ) {
			statement = parseInsert();
		} else if ( matches( "DELETE" ) ) {
			statement = parseDelete();
		} else if ( matches( "UPDATE" ) ) {
			statement = parseUpdate();
		} else {
			throw new UnexpectedTokenException( "statement", token );
		}
		// Resolve unknown references
		SqlWalker.INSTANCE.walk( new SqlVisitor() {

			private void convertToLiteral( Reference reference, SqlObject parent, Field field, int index ) {
				try {
					Object val = field.get( parent );
					if ( val instanceof List ) {
						List< SqlObject > list = ( List< SqlObject > ) val;
						list.set( index, new Literal( parent, reference.text ) );
					} else {
						field.set( parent, new Literal( parent, reference.text ) );
					}
				} catch ( IllegalAccessException error ) {
					throw new RuntimeException( error );
				}
			}

			private boolean resolve( Reference reference, String name, boolean ignoreColumns, SqlObject scopeObject ) {
				name = name.toLowerCase();
				reference.referent = scopeObject.localObjectInScope( name, ignoreColumns );
				if ( reference.referent != null ) {
					return true;
				}
				reference.referent = globalObjectsByName.get( name );
				if ( reference.referent != null ) {
					return true;
				}
				Statement statement = scopeObject.statement();
				if ( statement instanceof DmlStatement ) {
					for ( Reference globalRef : ( ( DmlStatement ) statement ).tables ) {
						if ( globalRef.unquoted().startsWith( "HT_" ) ) { // Temporary table
							continue;
						}
						reference.referent =
								globalObjectsByName.get( ( ( NamedObject ) globalRef.referent ).name().unquoted().toLowerCase()
										+ '.' + name );
						if ( reference.referent != null ) {
							return true;
						}
					}
				} else if ( statement instanceof AlterTable ) {
					reference.referent =
							globalObjectsByName.get( ( ( NamedObject ) ( ( AlterTable ) statement ).table.referent ).name().unquoted().toLowerCase()
									+ '.' + name );
					if ( reference.referent != null ) {
						return true;
					}
				}
				return false;
			}

			@Override
			public boolean visit( Object object, SqlObject parent, Field field, int index ) {
				if ( !( object instanceof Reference ) ) {
					return true;
				}
				Reference ref = ( Reference ) object;
				if ( ref.referent != null ) {
					return true;
				}
				String name = ref.unquoted();
				if ( name.startsWith( "HT_" ) ) { // Temporary table
					return true;
				}
				int ndx = name.indexOf( '.' );
				if ( ndx > 0 ) {
					String qualifier = name.substring( 0, ndx ).toLowerCase();
					Alias alias = ( Alias ) ref.localObjectInScope( qualifier, true );
					CreateTable table = null;
					if ( alias == null ) {
						table = ( CreateTable ) globalObjectsByName.get( qualifier );
					} else {
						if ( alias.reference.referent instanceof Select ) {
							if ( resolve( ref, name.substring( ndx + 1 ), false, alias.reference.referent ) ) {
								return true;
							}
							convertToLiteral( ref, parent, field, index );
							return true;
						}
						table = ( CreateTable ) alias.reference.referent;
					}
					if ( table != null ) {
						name = table.name().unquoted() + '.' + name.substring( ndx + 1 );
						setGlobalReferent( ref, name );
						return true;
					}
				} else if ( resolve( ref, name, false, ref ) ) {
					return true;
				}
				convertToLiteral( ref, parent, field, index );
				return true;
			}
		},
				statement );

		return statement;
	}

	private void parseAlias( Aliasable aliasable ) {
		aliasable.alias = new Alias( aliasable, token );
		aliasable.alias.localScope().mapLocalObjectByName( aliasable.alias.unquoted(), aliasable.alias );
	}

	private Aliasable parseAliasable( SqlObject select ) {
		Aliasable aliasable = new Aliasable( select );
		aliasable.expression = parseExpression( aliasable );
		if ( matches( "AS" ) ) {
			next();
			parseAlias( aliasable );
		} else if ( endOfExpression() ) {
			return aliasable;
		} else {
			parseAlias( aliasable );
		}
		if ( aliasable.expression instanceof Reference ) {
			aliasable.alias.reference = ( Reference ) aliasable.expression;
		}
		next();
		return aliasable;
	}

	private Aliasable parseAliasable( Select select, SqlObject parent ) {
		Aliasable aliasable = parseAliasable( parent );
		if ( aliasable.expression instanceof Reference ) {
			select.tables.add( setGlobalReferent( ( Reference ) aliasable.expression ) );
			if ( aliasable.alias != null ) {
				setGlobalReferent( aliasable.alias.reference );
			}
		} else if ( aliasable.expression instanceof Parentheses && aliasable.alias != null ) {
			SqlObject obj = ( ( Parentheses ) aliasable.expression ).operands.get( 0 );
			if ( obj instanceof Select ) {
				aliasable.alias.reference = new Reference( aliasable.alias, null );
				aliasable.alias.reference.referent = obj;
			}
		}
		return aliasable;
	}

	private Statement parseAlterTable() {
		next();
		if ( matches( "TABLE" ) ) {
			AlterTable alter = new AlterTable();
			alter.table = newGlobalReference( alter, next() );
			nextAndMatches( "ADD", "CONSTRAINT" );
			String name = next();
			next();
			if ( matches( "FOREIGN" ) ) {
				ForeignKey key = new ForeignKey( alter );
				alter.constraint = key;
				setGlobalName( name, key );
				parseKey( key.columns(), key );
				nextAndMatches( "REFERENCES" );
				key.references = newGlobalReference( key, next() );
			} else if ( matches( "UNIQUE" ) ) {
				alter.constraint = new Unique( alter );
				setGlobalName( name, alter.constraint );
				next();
				parseColumns( alter.constraint.columns(), alter.constraint );
			}
			return alter;
		}
		throw new UnexpectedTokenException( "alter type", token );
	}

	private Call parseCall() {
		Call call = new Call();
		next();
		if ( matches( "NEXT" ) ) {
			nextAndMatches( "VALUE", "FOR" );
			NextValueFor function = new NextValueFor( call );
			call.function = function;
			function.parameters.add( newGlobalReference( function, next() ) );
		} else if ( nextMatches( "(" ) ) {
			call.function = parseFunction( call );
		} else {
			throw new UnexpectedTokenException( "call procedure", token );
		}
		return call;
	}

	private Check parseCheck( SqlObject parent ) {
		Check check = new Check( parent, token );
		nextAndMatches( "(" );
		next();
		SqlObject expr = parseExpression( check );
		check.parameters.add( expr );
		next();
		return check;
	}

	private void parseColumns( List< Reference > columns, SqlObject parent ) {
		do {
			Reference ref = new Reference( parent, next() );
			ref.referent = parent.localObjectInScope( ref.unquoted(), false );
			columns.add( ref );
			next();
		} while ( matches( "," ) );
	}

	private Statement parseCreate() {
		next();
		if ( matches( "TABLE", "CACHED", "LOCAL", "GLOBAL", "TEMPORARY" ) ) {
			return parseCreateTable();
		}
		if ( matches( "SEQUENCE" ) ) {
			return parseCreateSequence();
		}
		if ( matches( "INDEX" ) ) {
			return parseCreateIndex();
		}
		if ( matches( "ALIAS" ) ) {
			return parseCreateAlias();
		}
		throw new UnexpectedTokenException( "create type", token );
	}

	private CreateAlias parseCreateAlias() {
		CreateAlias alias = new CreateAlias();
		setGlobalName( next(), alias );
		nextAndMatches( "AS" );
		alias.sourceCode = next();
		for ( next(); !matches( ";" ); next() ) {
			alias.sourceCode = token;
		}
		return alias;
	}

	private CreateIndex parseCreateIndex() {
		CreateIndex index = new CreateIndex();
		index.setName( new Name( index, next() ) );
		nextAndMatches( "ON" );
		index.table = newGlobalReference( index, next() );
		nextAndMatches( "(" );
		do {
			Reference ref = newGlobalReference( index, index.table, next() );
			index.columns.add( ref );
			next();
		} while ( matches( "," ) );
		next();
		return index;
	}

	private CreateSequence parseCreateSequence() {
		CreateSequence sequence = new CreateSequence();
		setGlobalName( next(), sequence );
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

	private CreateTable parseCreateTable() {
		CreateTable table = new CreateTable();
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
		setGlobalName( token, table );
		for ( nextAndMatches( "(" ); !matches( ")" ); ) {
			next();
			if ( matches( "PRIMARY" ) ) {
				PrimaryKey key = new PrimaryKey( table );
				table.constraints.add( key );
				parseKey( key.columns(), key );
				next();
			} else if ( matches( "CHECK" ) ) {
				Check check = parseCheck( table );
				table.constraints.add( check );
			} else {
				Column column = new Column( table );
				table.columns.add( column );
				column.setName( new Name( column, token ) );
				mapGlobalObjectByName( column );
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
					column.check = parseCheck( column );
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

	private Delete parseDelete() {
		Delete delete = new Delete();
		nextAndMatches( "FROM" );
		delete.tables.add( newGlobalReference( delete, next() ) );
		next();
		if ( matches( "WHERE" ) ) {
			next();
			delete.where = parseExpression( delete );
		}
		return delete;
	}

	private SqlObject parseExpression( SqlObject parent ) {
		return parseExpression( parent, null );
	}

	private SqlObject parseExpression( SqlObject parent, Operation previousOperation ) {
		Operation operation = operation( parent, true );
		SqlObject expr;
		if ( operation instanceof UnaryOperation ) {
			next();
			if ( previousOperation != null && previousOperation.operator.equalsIgnoreCase( "IN" ) ) {
				ParentheticalOptionallyOrderedSet set = new ParentheticalOptionallyOrderedSet( parent, operation );
				while ( !matches( ")" ) ) {
					if ( matches( "," ) ) {
						next();
					}
					expr = parseExpression( set );
					set.operands.add( expr );
				}
				next();
				expr = set;
			} else if ( operation instanceof Case ) {
				Case caseOp = ( Case ) operation;
				if ( !matches( "WHEN" ) ) {
					caseOp.expression = parseExpression( caseOp );
					next();
				}
				while ( matches( "WHEN" ) ) {
					When when = new When( caseOp );
					caseOp.operands.add( when );
					next();
					when.condition = parseExpression( when );
					next();
					when.then = parseExpression( when );
					if ( matches( "ELSE" ) ) {
						next();
						when.elseExpression = parseExpression( when );
					}
				}
				next();
				expr = caseOp;
			} else {
				expr = parseExpression( parent, operation );
				if ( operation instanceof Parentheses ) {
					next();
				}
			}
		} else if ( matches( "SELECT" ) ) {
			expr = parseSelect( parent );
		} else if ( matches( "{" ) || nextMatches( "(" ) ) {
			expr = parseFunction( parent );
		} else {
			if ( Character.isJavaIdentifierStart( Literal.unquoted( token ).charAt( 0 ) ) ) {
				expr = new Reference( parent, token );
			} else {
				expr = new Literal( parent, token );
			}
			next();
		}
		if ( endOfExpression() ) {
			if ( previousOperation == null ) {
				return expr;
			}
			expr.setParent( previousOperation );
			previousOperation.operands.add( expr );
			return previousOperation;
		}
		operation = operation( parent, false );
		if ( operation == null ) {
			return expr;
		}
		next();
		if ( previousOperation == null ) {
			expr.setParent( operation );
			operation.operands.add( expr );
			return parseExpression( parent, operation );
		}
		if ( operation.precedence < previousOperation.precedence ) {
			expr.setParent( previousOperation );
			previousOperation.operands.add( expr );
			if ( previousOperation.parent() instanceof Operation ) {
				Operation parentOperation = ( Operation ) previousOperation.parent();
				if ( parentOperation.operator.equalsIgnoreCase( operation.operator ) ) {
					operation = parentOperation;
					return parseExpression( parent, operation );
				}
			}
			if ( previousOperation instanceof Between ) {
				operation = previousOperation;
			} else {
				previousOperation.setParent( operation );
				operation.operands.add( previousOperation );
			}
			return parseExpression( parent, operation );
		}
		expr.setParent( operation );
		operation.operands.add( expr );
		operation.setParent( previousOperation );
		previousOperation.operands.add( operation );
		parseExpression( parent, operation );
		return previousOperation;
	}

	private Function parseFunction( SqlObject parent ) {
		Function function;
		if ( matches( "EXTRACT" ) ) {
			function = new Extract( parent, token );
		} else if ( matches( "COUNT" ) ) {
			function = new Count( parent, token );
		} else if ( matches( "CAST" ) ) {
			function = new Cast( parent, token );
		} else if ( matches( "TRIM" ) ) {
			function = new Trim( parent, token );
		} else if ( matches( "{" ) ) {
			nextAndMatches( "fn" );
			next();
			function = parseFunction( new OdbcFunction( parent, token ) );
			next();
			return function;
		} else {
			function = new Function( parent, token );
		}
		return parseFunction( function );
	}

	private Function parseFunction( Function function ) {
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
			SqlObject expr = parseExpression( function );
			function.parameters.add( expr );
		}
		next();
		return function;
	}

	private Insert parseInsert() {
		Insert insert = new Insert();
		nextAndMatches( "INTO" );
		Reference tableRef = newGlobalReference( insert, next() );
		insert.tables.add( tableRef );
		next();
		if ( matches( "SELECT" ) ) {
			insert.select = parseSelect( insert );
		} else {
			if ( matches( "(" ) ) {
				do {
					Reference ref = newGlobalReference( insert, tableRef, next() );
					insert.columns.add( ref );
					next();
				} while ( matches( "," ) );
				next();
			}
			if ( matches( "SELECT" ) ) {
				insert.select = parseSelect( insert );
			} else if ( matches( "VALUES" ) ) {
				nextAndMatches( "(" );
				next();
				while ( !matches( ")" ) ) {
					if ( matches( "," ) ) {
						next();
					}
					SqlObject expr = parseExpression( insert );
					insert.values.add( expr );
				}
				next();
			}
		}
		if ( matches( "WHERE" ) ) {
			next();
			insert.where = parseExpression( insert );
		}
		return insert;
	}

	private void parseKey( List< Reference > columns, SqlObject parent ) {
		nextAndMatches( "KEY" );
		next();
		parseColumns( columns, parent );
	}

	private Select parseSelect( SqlObject parent ) {
		Select select = new Select( parent );
		if ( nextMatches( "DISTINCT" ) ) {
			select.distinct = true;
			next();
		}
		while ( !matches( "FROM", ";" ) ) {
			next();
			select.columns.add( parseAliasable( select ) );
		}
		if ( matches( "FROM" ) ) {
			do {
				next();
				From from = new From( select );
				select.froms.add( from );
				from.aliasable = parseAliasable( select, from );
				while ( matches( "LEFT", "OUTER", "INNER", "CROSS" ) ) {
					Join join = new Join( from );
					from.joins.add( join );
					if ( matches( "LEFT" ) ) {
						join.left = true;
						next();
					}
					join.type = token;
					nextAndMatches( "JOIN" );
					next();
					join.aliasable = parseAliasable( select, join );
					if ( matches( "ON" ) ) {
						next();
						join.on = parseExpression( join );
					}
				}
			} while ( matches( "," ) );
		}
		if ( matches( "WHERE" ) ) {
			next();
			select.where = parseExpression( select );
		}
		if ( matches( "GROUP" ) ) {
			nextAndMatches( "BY" );
			select.groupBy = new Reference( select, next() );
			next();
		}
		if ( matches( "HAVING" ) ) {
			next();
			select.having = parseExpression( select );
		}
		if ( matches( "ORDER" ) ) {
			nextAndMatches( "BY" );
			do {
				OrderBy orderBy = new OrderBy( select );
				select.orderBy.add( orderBy );
				orderBy.column = new Reference( select, next() );
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
			select.limit = parseExpression( select );
		}
		if ( matches( "OFFSET" ) ) {
			next();
			select.offset = parseExpression( select );
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
			select.union = parseSelect( select );
		}
		return select;
	}

	private Update parseUpdate() {
		Update update = new Update();
		update.tables.add( newGlobalReference( update, next() ) );
		nextAndMatches( "SET" );
		do {
			next();
			update.sets.add( parseExpression( update ) );
		} while ( matches( "," ) );
		if ( matches( "WHERE" ) ) {
			next();
			update.where = parseExpression( update );
		}
		return update;
	}

	private boolean quoted( String token ) {
		return token.startsWith( "'" ) || token.startsWith( "\"" );
	}

	private void setGlobalName( String text, NamedObject object ) {
		object.setName( new Name( object, text ) );
		object.name().setParent( object );
		mapGlobalObjectByName( object );
	}

	private Reference setGlobalReferent( Reference reference ) {
		return setGlobalReferent( reference, reference.unquoted() );
	}

	Reference setGlobalReferent( Reference reference, String unquotedText ) {
		if ( unquotedText.startsWith( "HT_" ) ) { // Temporary table
			return reference;
		}
		reference.referent = globalObjectsByName.get( unquotedText.toLowerCase() );
		if ( reference.referent == null ) {
			throw new RuntimeException( "Invalid global reference: No SQL object exists named " + reference.text );
		}
		return reference;
	}
}
