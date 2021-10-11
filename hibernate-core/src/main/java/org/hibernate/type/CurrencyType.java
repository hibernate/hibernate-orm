/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Currency;

import org.hibernate.type.descriptor.java.CurrencyJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link Currency}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CurrencyType
		extends AbstractSingleColumnStandardBasicType<Currency> {

	public static final CurrencyType INSTANCE = new CurrencyType();

	public CurrencyType() {
		super( VarcharJdbcType.INSTANCE, CurrencyJavaTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "currency";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

}
