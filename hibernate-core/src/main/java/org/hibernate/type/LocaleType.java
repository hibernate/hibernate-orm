/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Locale;

import org.hibernate.type.descriptor.java.LocaleJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and @link Locale}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class LocaleType extends AbstractSingleColumnStandardBasicType<Locale> {

	public static final LocaleType INSTANCE = new LocaleType();

	public LocaleType() {
		super( VarcharJdbcType.INSTANCE, LocaleJavaTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "locale";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

}
