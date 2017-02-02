/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.InvalidPathException;
import org.hibernate.hql.internal.ast.tree.DotNode;
import org.hibernate.hql.internal.ast.tree.FromClause;
import org.hibernate.hql.internal.ast.tree.IdentNode;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.InFragment;
import org.hibernate.type.LiteralType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * A delegate that handles literals and constants for HqlSqlWalker, performing the token replacement functions and
 * classifying literals.
 *
 * @author josh
 */
public class LiteralProcessor implements HqlSqlTokenTypes {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			LiteralProcessor.class.getName()
	);

	/**
	 * In what format should Float and Double literal values be sent to the database?
	 */
	public static DecimalLiteralFormat DECIMAL_LITERAL_FORMAT = DecimalLiteralFormat.EXACT;

	private HqlSqlWalker walker;

	public LiteralProcessor(HqlSqlWalker hqlSqlWalker) {
		this.walker = hqlSqlWalker;
	}

	public boolean isAlias(String alias) {
		FromClause from = walker.getCurrentFromClause();
		while ( from.isSubQuery() ) {
			if ( from.containsClassAlias( alias ) ) {
				return true;
			}
			from = from.getParentFromClause();
		}
		return from.containsClassAlias( alias );
	}

	public void processConstant(AST constant, boolean resolveIdent) throws SemanticException {
		// If the constant is an IDENT, figure out what it means...
		boolean isIdent = ( constant.getType() == IDENT || constant.getType() == WEIRD_IDENT );
		if ( resolveIdent && isIdent && isAlias( constant.getText() ) ) {
			// IDENT is a class alias in the FROM.
			IdentNode ident = (IdentNode) constant;
			// Resolve to an identity column.
			ident.resolve( false, true );
		}
		else {
			// IDENT might be the name of a class.
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
			if ( InFragment.NULL.equals( discrim ) || InFragment.NOT_NULL.equals( discrim ) ) {
				throw new InvalidPathException(
						"subclass test not allowed for null or not null discriminator: '" + text + "'"
				);
			}
			// the class discriminator value
			setSQLValue( node, text, discrim );
		}
		else {
			Object value = ReflectHelper.getConstantValue( text, walker.getSessionFactoryHelper().getFactory() );
			if ( value == null ) {
				throw new InvalidPathException( "Invalid path: '" + text + "'" );
			}
			setConstantValue( node, text, value );
		}
	}

	private void setSQLValue(DotNode node, String text, String value) {
		LOG.debugf( "setSQLValue() %s -> %s", text, value );
		// Chop off the rest of the tree.
		node.setFirstChild( null );
		node.setType( SqlTokenTypes.SQL_TOKEN );
		node.setText( value );
		node.setResolvedConstant( text );
	}

	private void setConstantValue(DotNode node, String text, Object value) {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "setConstantValue() %s -> %s %s", text, value, value.getClass().getName() );
		}
		// Chop off the rest of the tree.
		node.setFirstChild( null );
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
			type = walker.getSessionFactoryHelper().getFactory().getTypeResolver().heuristicType(
					value.getClass().getName()
			);
		}
		catch (MappingException me) {
			throw new QueryException( me );
		}
		if ( type == null ) {
			throw new QueryException( QueryTranslator.ERROR_CANNOT_DETERMINE_TYPE + node.getText() );
		}
		try {
			LiteralType literalType = (LiteralType) type;
			Dialect dialect = walker.getSessionFactoryHelper().getFactory().getDialect();
			//noinspection unchecked
			node.setText( literalType.objectToSQLString( value, dialect ) );
		}
		catch (Exception e) {
			throw new QueryException( QueryTranslator.ERROR_CANNOT_FORMAT_LITERAL + node.getText(), e );
		}
		node.setDataType( type );
		node.setResolvedConstant( text );
	}

	public void processBoolean(AST constant) {
		String replacement = (String) walker.getTokenReplacements().get( constant.getText() );
		if ( replacement != null ) {
			constant.setText( replacement );
		}
	}

	public void processNull(AST constant) {
		constant.setText( "null" );
	}

	private void processLiteral(AST constant) {
		String replacement = (String) walker.getTokenReplacements().get( constant.getText() );
		if ( replacement != null ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "processConstant() : Replacing '%s' with '%s'", constant.getText(), replacement );
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
			LOG.unexpectedLiteralTokenType( literal.getType() );
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
				catch (NumberFormatException e) {
					LOG.tracev(
							"Could not format incoming text [{0}] as a NUM_INT; assuming numeric overflow and attempting as NUM_LONG",
							text
					);
				}
			}
			String literalValue = text;
			if ( literalValue.endsWith( "l" ) || literalValue.endsWith( "L" ) ) {
				literalValue = literalValue.substring( 0, literalValue.length() - 1 );
			}
			return Long.valueOf( literalValue ).toString();
		}
		catch (Throwable t) {
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

		final BigDecimal number;
		try {
			number = new BigDecimal( literalValue );
		}
		catch (Throwable t) {
			throw new HibernateException( "Could not parse literal [" + text + "] as big-decimal", t );
		}

		return DECIMAL_LITERAL_FORMAT.getFormatter().format( number );
	}


	private static interface DecimalFormatter {
		String format(BigDecimal number);
	}

	private static class ExactDecimalFormatter implements DecimalFormatter {
		public static final ExactDecimalFormatter INSTANCE = new ExactDecimalFormatter();

		public String format(BigDecimal number) {
			return number.toString();
		}
	}

	private static class ApproximateDecimalFormatter implements DecimalFormatter {
		public static final ApproximateDecimalFormatter INSTANCE = new ApproximateDecimalFormatter();

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
			catch (Throwable t) {
				throw new HibernateException(
						"Unable to format decimal literal in approximate format [" + number.toString() + "]",
						t
				);
			}
		}
	}

	public static enum DecimalLiteralFormat {
		/**
		 * Indicates that Float and Double literal values should
		 * be treated using the SQL "exact" format (i.e., '.001')
		 */
		EXACT {
			@Override
			public DecimalFormatter getFormatter() {
				return ExactDecimalFormatter.INSTANCE;
			}
		},
		/**
		 * Indicates that Float and Double literal values should
		 * be treated using the SQL "approximate" format (i.e., '1E-3')
		 */
		@SuppressWarnings({"UnusedDeclaration"})
		APPROXIMATE {
			@Override
			public DecimalFormatter getFormatter() {
				return ApproximateDecimalFormatter.INSTANCE;
			}
		};

		public abstract DecimalFormatter getFormatter();
	}

}
