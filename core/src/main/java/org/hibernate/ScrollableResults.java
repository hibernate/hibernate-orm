//$Id: ScrollableResults.java 6411 2005-04-13 07:37:50Z oneovthafew $
package org.hibernate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.type.Type;

/**
 * A result iterator that allows moving around within the results
 * by arbitrary increments. The <tt>Query</tt> / <tt>ScrollableResults</tt>
 * pattern is very similar to the JDBC <tt>PreparedStatement</tt>/
 * <tt>ResultSet</tt> pattern and the semantics of methods of this interface
 * are similar to the similarly named methods on <tt>ResultSet</tt>.<br>
 * <br>
 * Contrary to JDBC, columns of results are numbered from zero.
 *
 * @see Query#scroll()
 * @author Gavin King
 */
public interface ScrollableResults {
	/**
	 * Advance to the next result
	 * @return <tt>true</tt> if there is another result
	 */
	public boolean next() throws HibernateException;
	/**
	 * Retreat to the previous result
	 * @return <tt>true</tt> if there is a previous result
	 */
	public boolean previous() throws HibernateException;
	/**
	 * Scroll an arbitrary number of locations
	 * @param i a positive (forward) or negative (backward) number of rows
	 * @return <tt>true</tt> if there is a result at the new location
	 */
	public boolean scroll(int i) throws HibernateException;
	/**
	 * Go to the last result
	 * @return <tt>true</tt> if there are any results
	 */
	public boolean last() throws HibernateException;
	/**
	 * Go to the first result
	 * @return <tt>true</tt> if there are any results
	 */
	public boolean first() throws HibernateException;
	/**
	 * Go to a location just before first result (this is the initial location)
	 */
	public void beforeFirst() throws HibernateException;
	/**
	 * Go to a location just after the last result
	 */
	public void afterLast() throws HibernateException;
	/**
	 * Is this the first result?
	 *
	 * @return <tt>true</tt> if this is the first row of results
	 * @throws HibernateException
	 */
	public boolean isFirst() throws HibernateException;
	/**
	 * Is this the last result?
	 *
	 * @return <tt>true</tt> if this is the last row of results
	 * @throws HibernateException
	 */
	public boolean isLast() throws HibernateException;
	/**
	 * Release resources immediately.
	 */
	public void close() throws HibernateException;
	/**
	 * Get the current row of results
	 * @return an object or array
	 */
	public Object[] get() throws HibernateException;
	/**
	 * Get the <tt>i</tt>th object in the current row of results, without
	 * initializing any other results in the row. This method may be used
	 * safely, regardless of the type of the column (ie. even for scalar
	 * results).
	 * @param i the column, numbered from zero
	 * @return an object of any Hibernate type or <tt>null</tt>
	 */
	public Object get(int i) throws HibernateException;

	/**
	 * Get the type of the <tt>i</tt>th column of results
	 * @param i the column, numbered from zero
	 * @return the Hibernate type
	 */
	public Type getType(int i);

	/**
	 * Convenience method to read an <tt>integer</tt>
	 */
	public Integer getInteger(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>long</tt>
	 */
	public Long getLong(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>float</tt>
	 */
	public Float getFloat(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>boolean</tt>
	 */
	public Boolean getBoolean(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>double</tt>
	 */
	public Double getDouble(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>short</tt>
	 */
	public Short getShort(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>byte</tt>
	 */
	public Byte getByte(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>character</tt>
	 */
	public Character getCharacter(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>binary</tt>
	 */
	public byte[] getBinary(int col) throws HibernateException;
	/**
	 * Convenience method to read <tt>text</tt>
	 */
	public String getText(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>blob</tt>
	 */
	public Blob getBlob(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>clob</tt>
	 */
	public Clob getClob(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>string</tt>
	 */
	public String getString(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>big_decimal</tt>
	 */
	public BigDecimal getBigDecimal(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>big_integer</tt>
	 */
	public BigInteger getBigInteger(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>date</tt>, <tt>time</tt> or <tt>timestamp</tt>
	 */
	public Date getDate(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>locale</tt>
	 */
	public Locale getLocale(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>calendar</tt> or <tt>calendar_date</tt>
	 */
	public Calendar getCalendar(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>currency</tt>
	 */
	//public Currency getCurrency(int col) throws HibernateException;
	/**
	 * Convenience method to read a <tt>timezone</tt>
	 */
	public TimeZone getTimeZone(int col) throws HibernateException;
	/**
	 * Get the current location in the result set. The first
	 * row is number <tt>0</tt>, contrary to JDBC.
	 * @return the row number, numbered from <tt>0</tt>, or <tt>-1</tt> if
	 * there is no current row
	 */
	public int getRowNumber() throws HibernateException;
	/**
	 * Set the current location in the result set, numbered from either the
	 * first row (row number <tt>0</tt>), or the last row (row
	 * number <tt>-1</tt>).
	 * @param rowNumber the row number, numbered from the last row, in the
	 * case of a negative row number
	 * @return true if there is a row at that row number
	 */
	public boolean setRowNumber(int rowNumber) throws HibernateException;
}






