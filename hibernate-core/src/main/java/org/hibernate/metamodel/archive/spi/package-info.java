/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.archive.spi;
