/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations.common.reflection.java.generics;

import java.lang.reflect.Type;

/**
 * A typing context that knows how to "resolve" the generic parameters of a
 * <code>Type</code>.
 * <p/>
 * For example:
 * <p/>
 * <p/>
 * <blockquote>
 * <p/>
 * <pre>
 *  class Shop&ltT&gt{
 *    List&ltT&gt getCatalog() { ... }
 *  }
 * <p/>
 *  class Bakery extends Shop&ltBread&gt{}
 * </pre>
 * <p/>
 * </blockquote>
 * <p/>
 * Consider the type returned by method <code>getCatalog()</code>. There are
 * two possible contexts here. In the context of <code>Shop</code>, the type
 * is <code>List&ltT&gt</code>. In the context of <code>Bakery</code>, the
 * type is <code>List&ltBread&gt</code>. Each of these contexts can be
 * represented by a <code>TypeEnvironment</code>.
 *
 * @author Davide Marchignoli
 * @author Paolo Perrotta
 */
public interface TypeEnvironment {

	/**
	 * Binds as many generic components of the given type as possible in this
	 * context.
	 * <p/>
	 * Warning: if the returned <code>Type</code> is a <code>Class</code>,
	 * then it's guaranteed to be a regular Java <code>Class</code>. In all
	 * other cases, this method might return a custom implementation of some
	 * interface that extends <code>Type</code>. Be sure not to mix these
	 * objects with Java's implementations of <code>Type</code> to avoid
	 * potential identity problems.
	 * <p/>
	 * This class does not support bindings involving inner classes or
	 * upper/lower bounds.
	 *
	 * @return a type where the generic arguments have been replaced by raw
	 *         classes whenever this is possible.
	 */
	public Type bind(Type type);
}