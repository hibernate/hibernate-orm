/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Currency;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.CurrencyTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link Currency}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CurrencyType extends BasicTypeImpl<Currency> {

	public static final CurrencyType INSTANCE = new CurrencyType();

	protected CurrencyType() {
		super( CurrencyTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "currency";
	}

	@Override
	public JdbcLiteralFormatter<Currency> getJdbcLiteralFormatter() {
		return VarcharTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( CurrencyTypeDescriptor.INSTANCE );
	}
}
