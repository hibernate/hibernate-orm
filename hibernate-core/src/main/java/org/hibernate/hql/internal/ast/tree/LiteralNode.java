/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.hql.internal.ast.tree;

import java.sql.Types;
import javax.persistence.AttributeConverter;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.type.SingleColumnType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
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
		if ( expectedType != null ) {
			return expectedType;
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

		return ( (SingleColumnType) inherentType ).fromStringValue( text );
	}

	@Override
	public void setExpectedType(Type expectedType) {
		if ( this.expectedType != null ) {
			return;
		}

		if ( AttributeConverterTypeAdapter.class.isInstance( expectedType ) ) {
			final AttributeConverterTypeAdapter adapterType = (AttributeConverterTypeAdapter) expectedType;
			if ( getDataType().getReturnedClass().equals( adapterType.getModelType() ) ) {
				// apply the converter
				final AttributeConverter converter = ( (AttributeConverterTypeAdapter) expectedType ).getAttributeConverter();
				final Object converted = converter.convertToDatabaseColumn( getLiteralValue() );
				if ( isCharacterData( adapterType.sqlType() ) ) {
					setText( "'" + converted.toString() + "'" );
				}
				else {
					setText( converted.toString() );
				}
			}
			this.expectedType = expectedType;
		}
	}

	private boolean isCharacterData(int typeCode) {
		return Types.VARCHAR == typeCode
				|| Types.CHAR == typeCode
				|| Types.NVARCHAR == typeCode
				|| Types.NCHAR == typeCode;
	}

	@Override
	public Type getExpectedType() {
		return expectedType;
	}
}
