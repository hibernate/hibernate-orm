/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BooleanJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.CharJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CHAR CHAR(1)} and {@link Boolean} (using 'Y' and 'N')
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class YesNoType
		extends AbstractSingleColumnStandardBasicType<Boolean>
		implements ConvertedBasicType<Boolean> {

	public static final YesNoType INSTANCE = new YesNoType();

	public YesNoType() {
		super( CharJdbcTypeDescriptor.INSTANCE, BooleanJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "yes_no";
	}

	@Override
	public BasicValueConverter<Boolean, ?> getValueConverter() {
		return YesNoConverter.INSTANCE;
	}
}
