/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.persistence.Tuple;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.ScrollableResultsDeprecationsImplementor;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractScrollableResultsDeprecations<R> implements ScrollableResultsDeprecationsImplementor {

	protected abstract boolean isClosed();

	protected abstract R getCurrentRow();

	protected abstract ResultSetMapping getResultSetMapping();

	protected abstract SharedSessionContractImplementor getPersistenceContext();

	@Override
	public final Object get(int col) throws HibernateException {
		if ( isClosed() ) {
			throw new IllegalStateException( "ScrollableResults is closed" );
		}

		final R currentRow = getCurrentRow();

		// todo (6.0) : verify these checked conditions
		// 		much of this checking is new in 6.0 - make sure it fits

		if ( currentRow.getClass().isArray() ) {
			return ( (Object[]) currentRow )[col];
		}

		if ( Tuple.class.isInstance( currentRow ) ) {
			return ( (Tuple) currentRow ).get( col );
		}

		if ( List.class.isInstance( currentRow ) ) {
			// dynamic-instantiation with LIST target - `select new list(...) ...`
			return ( (List) currentRow ).get( col );
		}

		if ( col == 0 ) {
			// "magic" value
			return currentRow;
		}

		throw new HibernateException( "ScrollableResult row not defined as array, Tuple or List; cannot access positioned-results within the row" );
	}

	/**
	 * Check that the requested java type descriptor is identical with the
	 * result java type descriptor and return the typed value.
	 *
	 * This version makes sure the java type descriptors are identical.
	 *
	 * @see #get(int)
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	protected final Object getFinal(int col, JavaTypeDescriptor resultTypeDescriptor) {
		final Object value = get( col );
		if ( value == null ) {
			return null;
		}

		final JavaTypeDescriptor<?> valueType = getPersistenceContext().getFactory().getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( value.getClass() );

		if ( resultTypeDescriptor == valueType ) {
			return value;
		}

		throw createInvalidColumnTypeException( col, valueType, resultTypeDescriptor );
	}

	/**
	 * Check that the requested java type descriptor is compatible with the indexed
	 * result java type descriptor and returns the typed value.
	 *
	 * This version makes sure the java type descriptors are assignment compatible
	 *
	 * @see #get(int)
	 *
	 * @deprecated See deprecation notice on {@link #get(int)}
	 */
	@Deprecated
	protected final Object getNonFinal(int col, JavaTypeDescriptor resultType) {
		final Object value = get( col );

		if ( value == null ) {
			return null;
		}

		if ( resultType.isInstance( value ) ) {
			return value;
		}

		final JavaTypeDescriptor<?> valueType = getPersistenceContext().getFactory().getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( value.getClass() );
		throw createInvalidColumnTypeException( col,  valueType, resultType );
	}

	@Override
	public final BigDecimal getBigDecimal(int col) throws HibernateException {
		return (BigDecimal) getFinal( col, StandardSpiBasicTypes.BIG_DECIMAL.getJavaTypeDescriptor() );
	}

	@Override
	public final BigInteger getBigInteger(int col) throws HibernateException {
		return (BigInteger) getFinal( col, StandardSpiBasicTypes.BIG_INTEGER.getJavaTypeDescriptor() );
	}

	@Override
	public final byte[] getBinary(int col) throws HibernateException {
		return (byte[]) getFinal( col, StandardSpiBasicTypes.BINARY.getJavaTypeDescriptor() );
	}

	@Override
	public final String getText(int col) throws HibernateException {
		return (String) getFinal( col, StandardSpiBasicTypes.TEXT.getJavaTypeDescriptor() );
	}

	@Override
	public final Blob getBlob(int col) throws HibernateException {
		return (Blob) getNonFinal( col, StandardSpiBasicTypes.BLOB.getJavaTypeDescriptor() );
	}

	@Override
	public final Clob getClob(int col) throws HibernateException {
		return (Clob) getNonFinal( col, StandardSpiBasicTypes.CLOB.getJavaTypeDescriptor() );
	}

	@Override
	public final Boolean getBoolean(int col) throws HibernateException {
		return (Boolean) getFinal( col, StandardSpiBasicTypes.BOOLEAN.getJavaTypeDescriptor() );
	}

	@Override
	public final Byte getByte(int col) throws HibernateException {
		return (Byte) getFinal( col, StandardSpiBasicTypes.BYTE.getJavaTypeDescriptor() );
	}

	@Override
	public final Character getCharacter(int col) throws HibernateException {
		return (Character) getFinal( col, StandardSpiBasicTypes.CHARACTER.getJavaTypeDescriptor() );
	}

	@Override
	public final Date getDate(int col) throws HibernateException {
		return (Date) getNonFinal( col, StandardSpiBasicTypes.TIMESTAMP.getJavaTypeDescriptor() );
	}

	@Override
	public final Calendar getCalendar(int col) throws HibernateException {
		return (Calendar) getNonFinal( col, StandardSpiBasicTypes.CALENDAR.getJavaTypeDescriptor() );
	}

	@Override
	public final Double getDouble(int col) throws HibernateException {
		return (Double) getFinal( col, StandardSpiBasicTypes.DOUBLE.getJavaTypeDescriptor() );
	}

	@Override
	public final Float getFloat(int col) throws HibernateException {
		return (Float) getFinal( col, StandardSpiBasicTypes.FLOAT.getJavaTypeDescriptor() );
	}

	@Override
	public final Integer getInteger(int col) throws HibernateException {
		return (Integer) getFinal( col, StandardSpiBasicTypes.INTEGER.getJavaTypeDescriptor() );
	}

	// todo (6.0) : add TupleImplementor/TupleElementImplementor extensions dealing with our JavaTypeDescriptor
	//		namely:
	//			* javax.persistence.Tuple
	//			* javax.persistence.TupleElement
	//
	// odd place for this ^^, I know ;)
	//		but it all kind of ties together in support for deal with
	// 		dy-typed models (envers, e.g.)

	@Override
	public final Long getLong(int col) throws HibernateException {
		return (Long) getFinal( col, StandardSpiBasicTypes.LONG.getJavaTypeDescriptor() );
	}

	@Override
	public final Short getShort(int col) throws HibernateException {
		return (Short) getFinal( col, StandardSpiBasicTypes.SHORT.getJavaTypeDescriptor() );
	}

	@Override
	public final String getString(int col) throws HibernateException {
		return (String) getFinal( col, StandardSpiBasicTypes.STRING.getJavaTypeDescriptor() );
	}

	@Override
	public final Locale getLocale(int col) throws HibernateException {
		return (Locale) getFinal( col, StandardSpiBasicTypes.LOCALE.getJavaTypeDescriptor() );
	}

	@Override
	public final TimeZone getTimeZone(int col) throws HibernateException {
		return (TimeZone) getNonFinal( col, StandardSpiBasicTypes.TIMEZONE.getJavaTypeDescriptor() );
	}


	@Override
	public final org.hibernate.type.descriptor.java.JavaTypeDescriptor getType(int i) {
		return getResultSetMapping().getQueryResults().get( i ).getJavaTypeDescriptor();
	}

	protected HibernateException createInvalidColumnTypeException(
			int i,
			JavaTypeDescriptor type,
			JavaTypeDescriptor returnType) throws HibernateException {
		throw new HibernateException(
				"incompatible column types: " +
						type.getTypeName() +
						", " +
						returnType.getTypeName()
		);
	}
}
