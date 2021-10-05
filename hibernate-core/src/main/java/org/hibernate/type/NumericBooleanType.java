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
import org.hibernate.type.descriptor.java.IntegerJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#INTEGER INTEGER} and {@link Boolean} (using 1 and 0)
 *
 * @author Steve Ebersole
 */
public class NumericBooleanType 
		extends AbstractSingleColumnStandardBasicType<Boolean>
		implements ConvertedBasicType<Boolean> {

	public static final NumericBooleanType INSTANCE = new NumericBooleanType();
	public static final NumericConverter CONVERTER = new NumericConverter();

	public NumericBooleanType() {
		super( IntegerJdbcTypeDescriptor.INSTANCE, BooleanJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "numeric_boolean";
	}

	@Override
	public CastType getCastType() {
		return CastType.INTEGER_BOOLEAN;
	}

	@Override
	public BasicValueConverter<Boolean, ?> getValueConverter() {
		return CONVERTER;
	}

	public static class NumericConverter implements BasicValueConverter<Boolean, Integer> {
		/**
		 * Singleton access
		 */
		public static final NumericConverter INSTANCE = new NumericConverter();

		@Override
		public Boolean toDomainValue(Integer relationalForm) {
			return toDomain( relationalForm );
		}

		public static Boolean toDomain(Integer relationalForm) {
			if ( relationalForm == null ) {
				return null;
			}

			if ( 1 == relationalForm ) {
				return true;
			}

			if ( 0 == relationalForm ) {
				return false;
			}

			return null;
		}

		@Override
		public Integer toRelationalValue(Boolean domainForm) {
			return toRelational( domainForm );
		}

		public static Integer toRelational(Boolean domainForm) {
			if ( domainForm == null ) {
				return null;
			}

			return domainForm ? 1 : 0;
		}

		@Override
		public JavaTypeDescriptor<Boolean> getDomainJavaDescriptor() {
			return BooleanJavaTypeDescriptor.INSTANCE;
		}

		@Override
		public JavaTypeDescriptor<Integer> getRelationalJavaDescriptor() {
			return IntegerJavaTypeDescriptor.INSTANCE;
		}
	}
}
