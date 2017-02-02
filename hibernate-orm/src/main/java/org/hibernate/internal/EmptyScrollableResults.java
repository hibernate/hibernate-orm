/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.type.Type;

/**
 * @author Andrea Boriero
 */
public class EmptyScrollableResults implements ScrollableResultsImplementor {

	public static final ScrollableResultsImplementor INSTANCE = new EmptyScrollableResults();

	@Override
	public boolean isClosed() {
		return true;
	}

	@Override
	public int getNumberOfTypes() {
		return 0;
	}

	@Override
	public void close() {

	}

	@Override
	public boolean next() {
		return false;
	}

	@Override
	public boolean previous() {
		return false;
	}

	@Override
	public boolean scroll(int positions) {
		return false;
	}

	@Override
	public boolean last() {
		return true;
	}

	@Override
	public boolean first() {
		return false;
	}

	@Override
	public void beforeFirst() {

	}

	@Override
	public void afterLast() {

	}

	@Override
	public boolean isFirst() {
		return false;
	}

	@Override
	public boolean isLast() {
		return false;
	}

	@Override
	public int getRowNumber() {
		return 0;
	}

	@Override
	public boolean setRowNumber(int rowNumber) {
		return false;
	}

	@Override
	public Object[] get() {
		return new Object[0];
	}

	@Override
	public Object get(int i) {
		return null;
	}

	@Override
	public Type getType(int i) {
		return null;
	}

	@Override
	public Integer getInteger(int col) {
		return null;
	}

	@Override
	public Long getLong(int col) {
		return null;
	}

	@Override
	public Float getFloat(int col) {
		return null;
	}

	@Override
	public Boolean getBoolean(int col) {
		return null;
	}

	@Override
	public Double getDouble(int col) {
		return null;
	}

	@Override
	public Short getShort(int col) {
		return null;
	}

	@Override
	public Byte getByte(int col) {
		return null;
	}

	@Override
	public Character getCharacter(int col) {
		return null;
	}

	@Override
	public byte[] getBinary(int col) {
		return new byte[0];
	}

	@Override
	public String getText(int col) {
		return null;
	}

	@Override
	public Blob getBlob(int col) {
		return null;
	}

	@Override
	public Clob getClob(int col) {
		return null;
	}

	@Override
	public String getString(int col) {
		return null;
	}

	@Override
	public BigDecimal getBigDecimal(int col) {
		return null;
	}

	@Override
	public BigInteger getBigInteger(int col) {
		return null;
	}

	@Override
	public Date getDate(int col) {
		return null;
	}

	@Override
	public Locale getLocale(int col) {
		return null;
	}

	@Override
	public Calendar getCalendar(int col) {
		return null;
	}

	@Override
	public TimeZone getTimeZone(int col) {
		return null;
	}
}
