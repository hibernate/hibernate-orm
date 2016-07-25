package org.hibernate.type;


import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.GenericArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.CalendarTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.descriptor.java.DateTypeDescriptor;
import org.hibernate.type.descriptor.java.BigIntegerTypeDescriptor;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalDateJavaDescriptor;
import org.hibernate.type.descriptor.java.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.java.FloatTypeDescriptor;
import org.hibernate.type.descriptor.java.DoubleTypeDescriptor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.ShortTypeDescriptor;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.CalendarTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.JdbcDateTypeDescriptor.DateMutabilityPlan;
import org.hibernate.type.descriptor.sql.ArrayTypeDescriptor;


public class ArrayTypes<T> 
		extends AbstractSingleColumnStandardBasicType<T[]>
		implements LiteralType<T[]> {

	public static final ArrayTypes BOOLEAN = new ArrayTypes<>(BooleanTypeDescriptor.INSTANCE);

	public static final ArrayTypes INTEGER = new ArrayTypes<>(IntegerTypeDescriptor.INSTANCE);
	public static final ArrayTypes LONG = new ArrayTypes<>(LongTypeDescriptor.INSTANCE);
	public static final ArrayTypes FLOAT = new ArrayTypes<>(FloatTypeDescriptor.INSTANCE);
	public static final ArrayTypes DOUBLE = new ArrayTypes<>(DoubleTypeDescriptor.INSTANCE);

	public static final ArrayTypes BIGINTEGER = new ArrayTypes<>(BigIntegerTypeDescriptor.INSTANCE);
	public static final ArrayTypes BIGDECIMAL = new ArrayTypes<>(IntegerTypeDescriptor.INSTANCE);
	
	public static final ArrayTypes DATE = new ArrayTypes<>(DateTypeDescriptor.INSTANCE, DateMutabilityPlan.INSTANCE);
	public static final ArrayTypes CALENDAR = new ArrayTypes<>(CalendarTypeDescriptor.INSTANCE);
	
	public static final ArrayTypes LOCALDATE = new ArrayTypes<>(LocalDateJavaDescriptor.INSTANCE);
	public static final ArrayTypes STRING = new ArrayTypes<>(StringTypeDescriptor.INSTANCE);

	public ArrayTypes(AbstractTypeDescriptor<T> baseDescriptor) {
		super(ArrayTypeDescriptor.INSTANCE, new GenericArrayTypeDescriptor<>(baseDescriptor));
	}
	public ArrayTypes(AbstractTypeDescriptor<T> baseDescriptor, MutabilityPlan<T> baseMutabilityPlan) {
		super(ArrayTypeDescriptor.INSTANCE, new GenericArrayTypeDescriptor<T>(baseDescriptor, baseMutabilityPlan));
	}

	@Override
	public String getName() {
		return Integer[].class.getName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public String objectToSQLString(T[] value, Dialect dialect) throws Exception {
		StringBuilder sb = new StringBuilder("{");
		for (T i : value) {
			if (i == null) {
				sb.append("null");
			}
			else {
				sb.append(dialect.quote(i.toString()));
			}
			sb.append(',');
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append('}');
		return sb.toString();
	}

}
