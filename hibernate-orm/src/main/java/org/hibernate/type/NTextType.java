/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.sql.LongNVarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#LONGNVARCHAR LONGNVARCHAR} and {@link String}
 *
 * @author Gavin King,
 * @author Bertrand Renuart
 * @author Steve Ebersole
 */
public class NTextType extends AbstractSingleColumnStandardBasicType<String> {
	public static final NTextType INSTANCE = new NTextType();

	public NTextType() {
		super( LongNVarcharTypeDescriptor.INSTANCE, StringTypeDescriptor.INSTANCE );
	}

	public String getName() { 
		return "ntext";
	}

}
