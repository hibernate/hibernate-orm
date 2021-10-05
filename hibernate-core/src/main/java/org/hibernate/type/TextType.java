/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#LONGVARCHAR LONGVARCHAR} and {@link String}
 *
 * @author Gavin King,
 * @author Bertrand Renuart
 * @author Steve Ebersole
 */
public class TextType
		extends AbstractSingleColumnStandardBasicType<String>
		implements AdjustableBasicType<String> {
	public static final TextType INSTANCE = new TextType();

	public TextType() {
		super( LongVarcharJdbcTypeDescriptor.INSTANCE, StringJavaTypeDescriptor.INSTANCE );
	}

	public String getName() { 
		return "text";
	}
}
