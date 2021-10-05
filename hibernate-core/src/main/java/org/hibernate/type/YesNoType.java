/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.Internal;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.CastType;
import org.hibernate.type.descriptor.java.BooleanJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
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
	private static final YesNoConverter CONVERTER = new YesNoConverter();

	public YesNoType() {
		super( CharJdbcTypeDescriptor.INSTANCE, BooleanJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "yes_no";
	}

	@Override
	public CastType getCastType() {
		return CastType.YN_BOOLEAN;
	}

	@Override
	public BasicValueConverter<Boolean, ?> getValueConverter() {
		return CONVERTER;
	}

	@Internal
	public static class YesNoConverter implements BasicValueConverter<Boolean, Character> {
		/**
		 * Singleton access
		 */
		public static final YesNoConverter INSTANCE = new YesNoConverter();

		@Override
		public Boolean toDomainValue(Character relationalForm) {
			return toDomain( relationalForm );
		}

		public static Boolean toDomain(Character relationalForm) {
			if ( relationalForm == null ) {
				return null;
			}

			if ( 'Y' == relationalForm ) {
				return true;
			}

			if ( 'N' == relationalForm ) {
				return false;
			}

			return null;
		}

		@Override
		public Character toRelationalValue(Boolean domainForm) {
			return toRelational( domainForm );
		}

		public static Character toRelational(Boolean domainForm) {
			if ( domainForm == null ) {
				return null;
			}

			return domainForm ? 'Y' : 'N';
		}

		@Override
		public JavaTypeDescriptor<Boolean> getDomainJavaDescriptor() {
			return BooleanJavaTypeDescriptor.INSTANCE;
		}

		@Override
		public JavaTypeDescriptor<Character> getRelationalJavaDescriptor() {
			return CharacterJavaTypeDescriptor.INSTANCE;
		}
	}
}
