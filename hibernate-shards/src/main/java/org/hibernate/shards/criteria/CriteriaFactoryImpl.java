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

package org.hibernate.shards.criteria;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.shards.session.ShardedSessionException;

/**
 * Implementation of the {@link CriteriaFactory} interface.
 * @see Session#createCriteria(Class)
 * @see Session#createCriteria(Class, String)
 * @see Session#createCriteria(String)
 * @see Session#createCriteria(String, String)
 *
 * @author maxr@google.com (Max Ross)
 */
public class CriteriaFactoryImpl implements CriteriaFactory {

  private enum MethodSig {
    CLASS,
    CLASS_AND_ALIAS,
    ENTITY,
    ENTITY_AND_ALIAS
  }

  // the signature of the createCriteria method we're going to invoke when
  // the event fires
  private final MethodSig methodSig;

  // the Class we'll use when we create the Criteria.  We look at methodSig to
  // see if we should use it.
  private final Class persistentClass;

  // the alias we'll use when we create the Criteria.  We look at methodSig to
  // see if we should use it.
  private final String alias;

  // the entity name we'll use when we create the Criteria.  We look at methodSig to
  // see if we should use it.
  private final String entityName;

  /**
   * Create a CriteriaFactoryImpl
   *
   * @param methodSig the signature of the createCriteria method we'll invoke
   * when the event fires.
   * @param persistentClass the {@link Class} of the {@link Criteria} we're creating.
   * Can be null.
   * @param alias the alias of the {@link Criteria} we're creating.  Can be null.
   * @param entityName the entity name of the {@link} Criteria we're creating.
   * Can be null.
   */
  private CriteriaFactoryImpl(
      MethodSig methodSig,
      /*@Nullable*/ Class persistentClass,
      /*@Nullable*/ String alias,
      /*@Nullable*/ String entityName) {
    this.methodSig = methodSig;
    this.persistentClass = persistentClass;
    this.alias = alias;
    this.entityName = entityName;
  }

  /**
   * Create a CriteriaFactoryImpl.
   *
   * @param persistentClass the {@link Class} of the {@link Criteria} we're creating.
   */
  public CriteriaFactoryImpl(Class persistentClass) {
    this(MethodSig.CLASS, persistentClass, null, null);
  }

  /**
   * Create a CriteriaFactoryImpl.
   *
   * @param persistentClass the {@link Class} of the {@link Criteria} we're creating.
   * @param alias the alias of the {@link Criteria} we're creating.
   */
  public CriteriaFactoryImpl(Class persistentClass, String alias) {
    this(MethodSig.CLASS_AND_ALIAS, persistentClass, alias, null);
  }

  /**
   * Create a CriteriaFactoryImpl.
   *
   * @param entityName the entity name of the {@link Criteria} we're creating.
   */
  public CriteriaFactoryImpl(String entityName) {
    this(MethodSig.ENTITY, null, null, entityName);
  }

  /**
   * Create a CriteriaFactoryImpl.
   *
   * @param entityName the entity name of the {@link Criteria} we're creating.
   * @param alias the alias of the {@link Criteria} we're creating.
   */
  public CriteriaFactoryImpl(String entityName, String alias) {
    this(MethodSig.ENTITY_AND_ALIAS, null, alias, entityName);
  }

  public Criteria createCriteria(Session session) {
    switch (methodSig) {
      case CLASS:
        return session.createCriteria(persistentClass);
      case CLASS_AND_ALIAS:
        return session.createCriteria(persistentClass, alias);
      case ENTITY:
        return session.createCriteria(entityName);
      case ENTITY_AND_ALIAS:
        return session.createCriteria(entityName, alias);
      default:
        throw new ShardedSessionException(
            "Unknown constructor type for criteria creation: " + methodSig);
    }
  }
}
