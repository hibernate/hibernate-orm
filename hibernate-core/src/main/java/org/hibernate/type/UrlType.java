/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.net.URL;

import org.hibernate.type.descriptor.java.UrlJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link URL}
 *
 * @author Steve Ebersole
 */
public class UrlType extends AbstractSingleColumnStandardBasicType<URL> {
	public static final UrlType INSTANCE = new UrlType();

	public UrlType() {
		super( VarcharJdbcType.INSTANCE, UrlJavaTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "url";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

}
