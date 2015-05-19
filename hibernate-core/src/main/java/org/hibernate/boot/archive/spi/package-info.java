/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines the SPI for support of "scanning" of "archives".
 * <p/>
 * Scanning might mean:<ul>
 *     <li>searching for classes/packages that define certain interfaces</li>
 *     <li>locating named resources</li>
 * </ul>
 * And "archive" might mean:<ul>
 *     <li>a {@code .jar} file</li>
 *     <li>an exploded directory</li>
 *     <li>an OSGi bundle</li>
 *     <li>etc</li>
 * </ul>
 */
package org.hibernate.boot.archive.spi;
