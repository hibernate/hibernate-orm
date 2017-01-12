/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.basic;

import java.util.Comparator;
import javax.persistence.AttributeConverter;

import org.hibernate.Incubating;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * Designed to act as a key in the registry of basic type instances in {@link BasicTypeRegistry}
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 *
 * @since 6.0
 */
@Incubating
public class RegistryKey {
	private final Class javaTypeClass;
	private final int jdbcCode;

	private final Class<? extends MutabilityPlan> mutabilityPlanClass;
	private final Class<? extends Comparator> comparatorClass;
	private final Class<? extends AttributeConverter> attributeConverterClass;
	private final Class<? extends JdbcLiteralFormatter> jdbcLiteralFormatterClass;

	public static RegistryKey from(BasicType type) {
		return from(
				type.getJavaTypeDescriptor(),
				type.getColumnMappings()[0].getSqlTypeDescriptor(),
				type.getMutabilityPlan(),
				type.getComparator(),
				type.getJdbcLiteralFormatter(),
				null
		);
	}

	public static RegistryKey from(
			JavaTypeDescriptor javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor) {
		return from(
				javaTypeDescriptor,
				sqlTypeDescriptor,
				javaTypeDescriptor.getMutabilityPlan(),
				javaTypeDescriptor.getComparator(),
				null,
				null
		);
	}

	public static RegistryKey from(
			JavaTypeDescriptor javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			Object converterReference) {
		return from(
				javaTypeDescriptor,
				sqlTypeDescriptor,
				javaTypeDescriptor.getMutabilityPlan(),
				javaTypeDescriptor.getComparator(),
				converterReference,
				null
		);
	}

	public static RegistryKey from(
			JavaTypeDescriptor javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator,
			Object converterReference,
			JdbcLiteralFormatter jdbcLiteralFormatter) {
		Class<? extends AttributeConverter> converterClass = null;
		if ( converterReference != null ) {
			if ( converterReference instanceof AttributeConverterDefinition ) {
				converterClass = ( (AttributeConverterDefinition) converterReference ).getAttributeConverter().getClass();
			}
			else if ( converterReference instanceof AttributeConverter ) {
				converterClass = ( (AttributeConverter) converterReference ).getClass();
			}
		}
		return new RegistryKey(
				javaTypeDescriptor.getJavaTypeClass(),
				sqlTypeDescriptor.getSqlType(),
				extraClassFromOptional( mutabilityPlan ),
				extraClassFromOptional( comparator ),
				converterClass,
				extraClassFromOptional( jdbcLiteralFormatter )
		);
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> extraClassFromOptional(T reference) {
		if ( reference == null ) {
			return null;
		}
		else {
			return (Class<T>) reference.getClass();
		}
	}

	private RegistryKey(
			Class javaTypeClass,
			int jdbcCode,
			Class<MutabilityPlan> mutabilityPlanClass,
			Class<Comparator> comparatorClass,
			Class<? extends AttributeConverter> attributeConverterClass,
			Class<JdbcLiteralFormatter> jdbcLiteralFormatterClass) {
		assert javaTypeClass != null;

		this.javaTypeClass = javaTypeClass;
		this.jdbcCode = jdbcCode;
		this.mutabilityPlanClass = mutabilityPlanClass;
		this.comparatorClass = comparatorClass;
		this.attributeConverterClass = attributeConverterClass;
		this.jdbcLiteralFormatterClass = jdbcLiteralFormatterClass;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof RegistryKey ) ) {
			return false;
		}

		final RegistryKey that = (RegistryKey) o;
		return jdbcCode == that.jdbcCode
				&& javaTypeClass.equals( that.javaTypeClass )
				&& sameOptionalClass( mutabilityPlanClass, that.mutabilityPlanClass )
				&& sameOptionalClass( comparatorClass, that.comparatorClass )
				&& sameOptionalClass( attributeConverterClass, that.attributeConverterClass )
				&& sameOptionalClass( jdbcLiteralFormatterClass, that.jdbcLiteralFormatterClass );
	}

	private boolean sameOptionalClass(Class mine, Class yours) {
		if ( mine == null ) {
			return yours == null;
		}
		else {
			return mine.equals( yours );
		}
	}

	@Override
	public int hashCode() {
		int result = javaTypeClass.hashCode();
		result = 31 * result + jdbcCode;
		result = 31 * result + ( mutabilityPlanClass != null ? mutabilityPlanClass.hashCode() : 0 );
		result = 31 * result + ( comparatorClass != null ? comparatorClass.hashCode() : 0 );
		result = 31 * result + ( attributeConverterClass != null ? attributeConverterClass.hashCode() : 0 );
		result = 31 * result + ( jdbcLiteralFormatterClass != null ? jdbcLiteralFormatterClass.hashCode() : 0 );
		return result;
	}
}