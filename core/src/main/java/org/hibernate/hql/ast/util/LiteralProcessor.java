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
package org.hibernate.hql.ast.util;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.hql.ast.HqlSqlWalker;
import org.hibernate.hql.ast.InvalidPathException;
import org.hibernate.hql.ast.tree.DotNode;
import org.hibernate.hql.ast.tree.FromClause;
import org.hibernate.hql.ast.tree.IdentNode;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.InFragment;
import org.hibernate.type.LiteralType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ReflectHelper;

import antlr.SemanticException;
import antlr.collections.AST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

/**
 * A delegate that handles literals and constants for HqlSqlWalker, performing the token replacement functions and
 * classifying literals.
 *
 * @author josh
 */
public class LiteralProcessor implements HqlSqlTokenTypes {
	/**
	 * Indicates that Float and Double literal values should
	 * be treated using the SQL "exact" format (i.e., '.001')
	 */
	public static final int EXACT = 0;
	/**
	 * Indicates that Float and Double literal values should
	 * be treated using the SQL "approximate" format (i.e., '1E-3')
	 */
	public static final int APPROXIMATE = 1;
	/**
	 * In what format should Float and Double literal values be sent
	 * to the database?
	 * @see #EXACT, #APPROXIMATE
	 */
	public static int DECIMAL_LITERAL_FORMAT = EXACT;

	private static final Logger log = LoggerFactory.getLogger( LiteralProcessor.class );

	private HqlSqlWalker walker;

	public LiteralProcessor(HqlSqlWalker hqlSqlWalker) {
		this.walker = hqlSqlWalker;
	}

	public boolean isAlias(String alias) {
		FromClause from = walker.getCurrentFromClause();
		while ( from.isSubQuery() ) {
			if ( from.containsClassAlias(alias) ) {
				return true;
			}
			from = from.getParentFromClause();
		}
		return from.containsClassAlias(alias);
	}

	public void processConstant(AST constant, boolean resolveIdent) throws SemanticException {
		// If the constant is an IDENT, figure out what it means...
		boolean isIdent = ( constant.getType() == IDENT || constant.getType() == WEIRD_IDENT );
		if ( resolveIdent && isIdent && isAlias( constant.getText() ) ) { // IDENT is a class alias in the FROM.
			IdentNode ident = ( IdentNode ) constant;
			// Resolve to an identity column.
			ident.resolve(false, true);
		}
		else {	// IDENT might be the name of a class.
			Queryable queryable = walker.getSessionFactoryHelper().findQueryableUsingImports( constant.getText() );
			if ( isIdent && queryable != null ) {
				constant.setText( queryable.getDiscriminatorSQLValue() );
			}
			// Otherwise, it's a literal.
			else {
				processLiteral( constant );
			}
		}
	}

	public void lookupConstant(DotNode node) throws SemanticException {
		String text = ASTUtil.getPathText( node );
		Queryable persister = walker.getSessionFactoryHelper().findQueryableUsingImports( text );
		if ( persister != null ) {
			// the name of an entity class
			final String discrim = persister.getDiscriminatorSQLValue();
			node.setDataType( persister.getDiscriminatorType() );
			if ( InFragment.NULL.equals(discrim) || InFragment.NOT_NULL.equals(discrim) ) {
				throw new InvalidPathException( "subclass test not allowed for null or not null discriminator: '" + text + "'" );
			}
			else {
				setSQLValue( node, text, discrim ); //the class discriminator value
			}
		}
		else {
			Object value = ReflectHelper.getConstantValue( text );
			if ( value == null ) {
				throw new InvalidPathException( "Invalid path: '" + text + "'" );
			}
			else {
				setConstantValue( node, text, value );
			}
		}
	}

	private void setSQLValue(DotNode node, String text, String value) {
		if ( log.isDebugEnabled() ) {
			log.debug( "setSQLValue() " + text + " -> " + value );
		}
		node.setFirstChild( null );	// Chop off the rest of the tree.
		node.setType( SqlTokenTypes.SQL_TOKEN );
		node.setText(value);
		node.setResolvedConstant( text );
	}

	private void setConstantValue(DotNode node, String text, Object value) {
		if ( log.isDebugEnabled() ) {
			log.debug( "setConstantValue() " + text + " -> " + value + " " + value.getClass().getName() );
		}
		node.setFirstChild( null );	// Chop off the rest of the tree.
		if ( value instanceof String ) {
			node.setType( SqlTokenTypes.QUOTED_STRING );
		}
		else if ( value instanceof Character ) {
			node.setType( SqlTokenTypes.QUOTED_STRING );
		}
		else if ( value instanceof Byte ) {
			node.setType( SqlTokenTypes.NUM_INT );
		}
		else if ( value instanceof Short ) {
			node.setType( SqlTokenTypes.NUM_INT );
		}
		else if ( value instanceof Integer ) {
			node.setType( SqlTokenTypes.NUM_INT );
		}
		else if ( value instanceof Long ) {
			node.setType( SqlTokenTypes.NUM_LONG );
		}
		else if ( value instanceof Double ) {
			node.setType( SqlTokenTypes.NUM_DOUBLE );
		}
		else if ( value instanceof Float ) {
			node.setType( SqlTokenTypes.NUM_FLOAT );
		}
		else {
			node.setType( SqlTokenTypes.CONSTANT );
		}
		Type type;
		try {
			type = TypeFactory.heuristicType( value.getClass().getName() );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
		if ( type == null ) {
			throw new QueryException( QueryTranslator.ERROR_CANNOT_DETERMINE_TYPE + node.getText() );
		}
		try {
			LiteralType literalType = ( LiteralType ) type;
			Dialect dialect = walker.getSessionFactoryHelper().getFactory().getDialect();
			node.setText( literalType.objectToSQLString( value, dialect ) );
		}
		catch ( Exception e ) {
			throw new QueryException( QueryTranslator.ERROR_CANNOT_FORMAT_LITERAL + node.getText(), e );
		}
		node.setDataType( type );
		node.setResolvedConstant( text );
	}

	public void processBoolean(AST constant) {
		// TODO: something much better - look at the type of the other expression!
		// TODO: Have comparisonExpression and/or arithmeticExpression rules complete the resolution of boolean nodes.
		String replacement = ( String ) walker.getTokenReplacements().get( constant.getText() );
		if ( replacement != null ) {
			constant.setText( replacement );
		}
		else {
			boolean bool = "true".equals( constant.getText().toLowerCase() );
			Dialect dialect = walker.getSessionFactoryHelper().getFactory().getDialect();
			constant.setText( dialect.toBooleanValueString(bool) );
		}
	}

	private void processLiteral(AST constant) {
		String replacement = ( String ) walker.getTokenReplacements().get( constant.getText() );
		if ( replacement != null ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "processConstant() : Replacing '" + constant.getText() + "' with '" + replacement + "'" );
			}
			constant.setText( replacement );
		}
	}

	public void processNumeric(AST literal) {
		if ( literal.getType() == NUM_INT
				|| literal.getType() == NUM_LONG
				|| literal.getType() == NUM_BIG_INTEGER ) {
			literal.setText( determineIntegerRepresentation( literal.getText(), literal.getType() ) );
		}
		else if ( literal.getType() == NUM_FLOAT
				|| literal.getType() == NUM_DOUBLE
				|| literal.getType() == NUM_BIG_DECIMAL ) {
			literal.setText( determineDecimalRepresentation( literal.getText(), literal.getType() ) );
		}
		else {
			log.warn( "Unexpected literal token type [" + literal.getType() + "] passed for numeric processing" );
		}
	}

	private String determineIntegerRepresentation(String text, int type) {
		try {
			if ( type == NUM_BIG_INTEGER ) {
				String literalValue = text;
				if ( literalValue.endsWith( "bi" ) || literalValue.endsWith( "BI" ) ) {
					literalValue = literalValue.substring( 0, literalValue.length() - 2 );
				}
				return new BigInteger( literalValue ).toString();
			}
			if ( type == NUM_INT ) {
				try {
					return Integer.valueOf( text ).toString();
				}
				catch( NumberFormatException e ) {
					log.trace( "could not format incoming text [" + text + "] as a NUM_INT; assuming numeric overflow and attempting as NUM_LONG" );
				}
			}
			String literalValue = text;
			if ( literalValue.endsWith( "l" ) || literalValue.endsWith( "L" ) ) {
				literalValue = literalValue.substring( 0, literalValue.length() - 1 );
			}
			return Long.valueOf( literalValue ).toString();
		}
		catch( Throwable t ) {
			throw new HibernateException( "Could not parse literal [" + text + "] as integer", t );
		}
	}

	public String determineDecimalRepresentation(String text, int type) {
		String literalValue = text;
		if ( type == NUM_FLOAT ) {
			if ( literalValue.endsWith( "f" ) || literalValue.endsWith( "F" ) ) {
				literalValue = literalValue.substring( 0, literalValue.length() - 1 );
			}
		}
		else if ( type == NUM_DOUBLE ) {
			if ( literalValue.endsWith( "d" ) || literalValue.endsWith( "D" ) ) {
				literalValue = literalValue.substring( 0, literalValue.length() - 1 );
			}
		}
		else if ( type == NUM_BIG_DECIMAL ) {
			if ( literalValue.endsWith( "bd" ) || literalValue.endsWith( "BD" ) ) {
				literalValue = literalValue.substring( 0, literalValue.length() - 2 );
			}
		}

		BigDecimal number = null;
		try {
			number = new BigDecimal( literalValue );
		}
		catch( Throwable t ) {
			throw new HibernateException( "Could not parse literal [" + text + "] as big-decimal", t );
		}

		return formatters[ DECIMAL_LITERAL_FORMAT ].format( number );
	}


	private static interface DecimalFormatter {
		String format(BigDecimal number);
	}

	private static class ExactDecimalFormatter implements DecimalFormatter {
		public String format(BigDecimal number) {
			return number.toString();
		}
	}

	private static class ApproximateDecimalFormatter implements DecimalFormatter {
		private static final String FORMAT_STRING = "#0.0E0";
		public String format(BigDecimal number) {
			try {
				// TODO : what amount of significant digits need to be supported here?
				//      - from the DecimalFormat docs:
				//          [significant digits] = [minimum integer digits] + [maximum fraction digits]
				DecimalFormat jdkFormatter = new DecimalFormat( FORMAT_STRING );
				jdkFormatter.setMinimumIntegerDigits( 1 );
				jdkFormatter.setMaximumFractionDigits( Integer.MAX_VALUE );
				return jdkFormatter.format( number );
			}
			catch( Throwable t ) {
				throw new HibernateException( "Unable to format decimal literal in approximate format [" + number.toString() + "]", t );
			}
		}
	}

	private static final DecimalFormatter[] formatters = new DecimalFormatter[] {
			new ExactDecimalFormatter(),
			new ApproximateDecimalFormatter()
	};
}