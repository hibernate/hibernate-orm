/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.annotations.lob;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.PrimitiveCharacterArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;

/**
 * A type that maps JDBC {@link java.sql.Types#LONGVARCHAR LONGVARCHAR} and {@code char[]}.
 * 
 * @author Strong Liu
 */
public class PrimitiveCharacterArrayTextType extends AbstractSingleColumnStandardBasicType<char[]> {
	public static final PrimitiveCharacterArrayTextType INSTANCE = new PrimitiveCharacterArrayTextType();

	public PrimitiveCharacterArrayTextType() {
		super( LongVarcharTypeDescriptor.INSTANCE, PrimitiveCharacterArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}
}
