/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.core;

/**
 * Hibernate EntityManager specific implementations of Hibernate event listeners.  Generally the listeners
 * here either:<ul>
 *     <li>provide tweaks to internal processing to conform with JPA spec</li>
 *     <li>bridge to JPA event callbacks</li>
 * </ul>
 */
