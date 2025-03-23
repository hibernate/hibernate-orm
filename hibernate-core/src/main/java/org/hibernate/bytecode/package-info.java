/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
