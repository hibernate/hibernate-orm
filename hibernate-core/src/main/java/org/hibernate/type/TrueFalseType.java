/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.CastType;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.CharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CHAR CHAR(1)} and {@link Boolean} (using 'T' and 'F')
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TrueFalseType
		extends AbstractSingleColumnStandardBasicType<Boolean>
		implements PrimitiveType<Boolean>, DiscriminatorType<Boolean>, ConvertedBasicType<Boolean> {

	public static final TrueFalseType INSTANCE = new TrueFalseType();
	private static final TrueFalseConverter CONVERTER = new TrueFalseConverter();

	public TrueFalseType() {
		super( CharTypeDescriptor.INSTANCE, new BooleanTypeDescriptor( 'T', 'F' ) );
	}

	@Override
	public String getName() {
		return "true_false";
	}

	@Override
	public Class getPrimitiveClass() {
		return boolean.class;
	}

	@Override
	public Boolean stringToObject(CharSequence sequence) throws Exception {
		return fromString( sequence );
	}

	@Override
	public Serializable getDefaultValue() {
		return Boolean.FALSE;
	}

	@Override
	public CastType getCastType() {
		return CastType.TF_BOOLEAN;
	}

	@Override
	public BasicValueConverter<Boolean, ?> getValueConverter() {
		return CONVERTER;
	}

	public static class TrueFalseConverter implements BasicValueConverter<Boolean, Character> {
		/**
		 * Singleton access
		 */
		public static final TrueFalseConverter INSTANCE = new TrueFalseConverter();

		@Override
		public Boolean toDomainValue(Character relationalForm) {
			return toDomain( relationalForm );
		}

		public static Boolean toDomain(Character relationalForm) {
			if ( relationalForm == null ) {
				return null;
			}

			if ( 'T' == relationalForm ) {
				return true;
			}

			if ( 'F' == relationalForm ) {
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

			return domainForm ? 'T' : 'F';
		}

		@Override
		public JavaTypeDescriptor<Boolean> getDomainJavaDescriptor() {
			return BooleanTypeDescriptor.INSTANCE;
		}

		@Override
		public JavaTypeDescriptor<Character> getRelationalJavaDescriptor() {
			return CharacterTypeDescriptor.INSTANCE;
		}
	}
}
