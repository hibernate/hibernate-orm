/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.CastType;
import org.hibernate.type.descriptor.java.BooleanJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.CharJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CHAR CHAR(1)} and {@link Boolean} (using 'T' and 'F')
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TrueFalseType
		extends AbstractSingleColumnStandardBasicType<Boolean>
		implements ConvertedBasicType<Boolean> {

	public static final TrueFalseType INSTANCE = new TrueFalseType();

	public TrueFalseType() {
		super( CharJdbcTypeDescriptor.INSTANCE, new BooleanJavaTypeDescriptor( 'T', 'F' ) );
	}

	@Override
	public String getName() {
		return "true_false";
	}

	@Override
	public BasicValueConverter<Boolean, ?> getValueConverter() {
		return TrueFalseConverter.INSTANCE;
	}

}
