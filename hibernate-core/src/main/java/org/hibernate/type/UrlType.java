/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.net.URL;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.UrlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link URL}
 *
 * @author Steve Ebersole
 */
public class UrlType extends AbstractSingleColumnStandardBasicType<URL> implements DiscriminatorType<URL> {
	public static final UrlType INSTANCE = new UrlType();

	public UrlType() {
		super( VarcharTypeDescriptor.INSTANCE, UrlTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "url";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public String toString(URL value) {
		return UrlTypeDescriptor.INSTANCE.toString( value );
	}

	public String objectToSQLString(URL value, Dialect dialect) throws Exception {
		return StringType.INSTANCE.objectToSQLString( toString( value ), dialect );
	}

	public URL stringToObject(String xml) throws Exception {
		return UrlTypeDescriptor.INSTANCE.fromString( xml );
	}
}
