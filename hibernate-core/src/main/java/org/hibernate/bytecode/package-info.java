/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * This package defines an SPI for integrating bytecode libraries with Hibernate.
 * A bytecode library is used for:
 * <ol>
 * 		<li>
 * 			<b>Reflection optimization</b> - to speed up the performance of entity
 * 			and component construction and field/property access,
 * 		</li>
 * 		<li>
 * 			<b>Proxy generation</b> - runtime building of proxies used to defer
 * 			loading of lazy entities, and
 * 		</li>
 * 		<li>
 * 			<b>Field-level interception</b> - build-time instrumentation of entity
 * 			classes for the purpose of intercepting field-level access, for both
 * 			lazy loading and dirty tracking.
 * 		</li>
 * </ol>
 */
package org.hibernate.bytecode;
