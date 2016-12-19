/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.sql.Types;
import java.util.Locale;
import javax.persistence.AttributeConverter;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import antlr.SemanticException;

/**
 * Represents a literal.
 *
 * @author josh
 * @author Steve Ebersole
 */
public class LiteralNode extends AbstractSelectExpression implements HqlSqlTokenTypes, ExpectedTypeAwareNode {
	private Type expectedType;

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	public Type getDataType() {
		if ( getExpectedType() != null ) {
			return getExpectedType();
		}

		switch ( getType() ) {
			case NUM_INT:
				return StandardBasicTypes.INTEGER;
			case NUM_FLOAT:
				return StandardBasicTypes.FLOAT;
			case NUM_LONG:
				return StandardBasicTypes.LONG;
			case NUM_DOUBLE:
				return StandardBasicTypes.DOUBLE;
			case NUM_BIG_INTEGER:
				return StandardBasicTypes.BIG_INTEGER;
			case NUM_BIG_DECIMAL:
				return StandardBasicTypes.BIG_DECIMAL;
			case QUOTED_STRING:
				return StandardBasicTypes.STRING;
			case TRUE:
			case FALSE:
				return StandardBasicTypes.BOOLEAN;
			default:
				return null;
		}
	}

	public Object getLiteralValue() {
		String text = getText();
		if ( getType() == QUOTED_STRING ) {
			text = text.substring( 1, text.length() -1 );
		}

		final Type inherentType = getDataType();
		if ( inherentType == null ) {
			return text;
		}

		return inherentType.getJavaTypeDescriptor().fromString( text );
	}

	@Override
	public void setExpectedType(Type expectedType) {
		if ( this.expectedType != null ) {
			return;
		}

		this.expectedType = expectedType;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		final JdbcLiteralFormatter jdbcLiteralFormatter = getExpectedType().getJdbcLiteralFormatter();
		if ( jdbcLiteralFormatter == null ) {
			// There was no literal formatter associated with the
			// todo : develop a "base" JdbcLiteralFormatter for standard basic types?
			//		- Also would be nice to somehow consider the SqlTypeDescriptor in this
			//			rendering process.  That's a better indicator of whether enclosing in
			//			quotes is needed.
			//
			// for now, it is simply illegal to not have one...
			throw new HibernateException( "LiteralNode, but Type has no JdbcLiteralFormatter associated with it: " + expectedType );
		}

		return jdbcLiteralFormatter.toJdbcLiteral( getLiteralValue(), sessionFactory.getJdbcServices().getJdbcEnvironment().getDialect() );
	}

	@Override
	public Type getExpectedType() {
		return expectedType;
	}
}
