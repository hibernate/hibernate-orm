/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.defaultmock;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Maulik Shah
 */
public class QueryDefaultMock implements Query {

  public String getQueryString() {
    throw new UnsupportedOperationException();
  }

  public Type[] getReturnTypes() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public String[] getReturnAliases() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public String[] getNamedParameters() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Iterator iterate() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ScrollableResults scroll() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public List list() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object uniqueResult() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public int executeUpdate() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query setMaxResults(int maxResults) {
    throw new UnsupportedOperationException();
  }

  public Query setFirstResult(int firstResult) {
    throw new UnsupportedOperationException();
  }

  public Query setReadOnly(boolean readOnly) {
    throw new UnsupportedOperationException();
  }

  public Query setCacheable(boolean cacheable) {
    throw new UnsupportedOperationException();
  }

  public Query setCacheRegion(String cacheRegion) {
    throw new UnsupportedOperationException();
  }

  public Query setTimeout(int timeout) {
    throw new UnsupportedOperationException();
  }

  public Query setFetchSize(int fetchSize) {
    throw new UnsupportedOperationException();
  }

  public Query setLockMode(String alias, LockMode lockMode) {
    throw new UnsupportedOperationException();
  }

  public Query setComment(String comment) {
    throw new UnsupportedOperationException();
  }

  public Query setFlushMode(FlushMode flushMode) {
    throw new UnsupportedOperationException();
  }

  public Query setCacheMode(CacheMode cacheMode) {
    throw new UnsupportedOperationException();
  }

  public Query setParameter(int position, Object val, Type type) {
    throw new UnsupportedOperationException();
  }

  public Query setParameter(String name, Object val, Type type) {
    throw new UnsupportedOperationException();
  }

  public Query setParameter(int position, Object val) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query setParameter(String name, Object val) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query setParameters(Object[] values, Type[] types) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query setParameterList(String name, Collection vals, Type type) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query setParameterList(String name, Collection vals) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query setParameterList(String name, Object[] vals, Type type) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query setParameterList(String name, Object[] vals) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query setProperties(Object bean) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query setString(int position, String val) {
    throw new UnsupportedOperationException();
  }

  public Query setCharacter(int position, char val) {
    throw new UnsupportedOperationException();
  }

  public Query setBoolean(int position, boolean val) {
    throw new UnsupportedOperationException();
  }

  public Query setByte(int position, byte val) {
    throw new UnsupportedOperationException();
  }

  public Query setShort(int position, short val) {
    throw new UnsupportedOperationException();
  }

  public Query setInteger(int position, int val) {
    throw new UnsupportedOperationException();
  }

  public Query setLong(int position, long val) {
    throw new UnsupportedOperationException();
  }

  public Query setFloat(int position, float val) {
    throw new UnsupportedOperationException();
  }

  public Query setDouble(int position, double val) {
    throw new UnsupportedOperationException();
  }

  public Query setBinary(int position, byte[] val) {
    throw new UnsupportedOperationException();
  }

  public Query setText(int position, String val) {
    throw new UnsupportedOperationException();
  }

  public Query setSerializable(int position, Serializable val) {
    throw new UnsupportedOperationException();
  }

  public Query setLocale(int position, Locale locale) {
    throw new UnsupportedOperationException();
  }

  public Query setBigDecimal(int position, BigDecimal number) {
    throw new UnsupportedOperationException();
  }

  public Query setBigInteger(int position, BigInteger number) {
    throw new UnsupportedOperationException();
  }

  public Query setDate(int position, Date date) {
    throw new UnsupportedOperationException();
  }

  public Query setTime(int position, Date date) {
    throw new UnsupportedOperationException();
  }

  public Query setTimestamp(int position, Date date) {
    throw new UnsupportedOperationException();
  }

  public Query setCalendar(int position, Calendar calendar) {
    throw new UnsupportedOperationException();
  }

  public Query setCalendarDate(int position, Calendar calendar) {
    throw new UnsupportedOperationException();
  }

  public Query setString(String name, String val) {
    throw new UnsupportedOperationException();
  }

  public Query setCharacter(String name, char val) {
    throw new UnsupportedOperationException();
  }

  public Query setBoolean(String name, boolean val) {
    throw new UnsupportedOperationException();
  }

  public Query setByte(String name, byte val) {
    throw new UnsupportedOperationException();
  }

  public Query setShort(String name, short val) {
    throw new UnsupportedOperationException();
  }

  public Query setInteger(String name, int val) {
    throw new UnsupportedOperationException();
  }

  public Query setLong(String name, long val) {
    throw new UnsupportedOperationException();
  }

  public Query setFloat(String name, float val) {
    throw new UnsupportedOperationException();
  }

  public Query setDouble(String name, double val) {
    throw new UnsupportedOperationException();
  }

  public Query setBinary(String name, byte[] val) {
    throw new UnsupportedOperationException();
  }

  public Query setText(String name, String val) {
    throw new UnsupportedOperationException();
  }

  public Query setSerializable(String name, Serializable val) {
    throw new UnsupportedOperationException();
  }

  public Query setLocale(String name, Locale locale) {
    throw new UnsupportedOperationException();
  }

  public Query setBigDecimal(String name, BigDecimal number) {
    throw new UnsupportedOperationException();
  }

  public Query setBigInteger(String name, BigInteger number) {
    throw new UnsupportedOperationException();
  }

  public Query setDate(String name, Date date) {
    throw new UnsupportedOperationException();
  }

  public Query setTime(String name, Date date) {
    throw new UnsupportedOperationException();
  }

  public Query setTimestamp(String name, Date date) {
    throw new UnsupportedOperationException();
  }

  public Query setCalendar(String name, Calendar calendar) {
    throw new UnsupportedOperationException();
  }

  public Query setCalendarDate(String name, Calendar calendar) {
    throw new UnsupportedOperationException();
  }

  public Query setEntity(int position, Object val) {
    throw new UnsupportedOperationException();
  }

  public Query setEntity(String name, Object val) {
    throw new UnsupportedOperationException();
  }

  public Query setResultTransformer(ResultTransformer transformer) {
    throw new UnsupportedOperationException();
  }

  public Query setProperties(Map bean) throws HibernateException {
    throw new UnsupportedOperationException();
  }
}
