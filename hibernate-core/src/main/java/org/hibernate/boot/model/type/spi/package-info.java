/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines the SPI contracts for type information handling during bootstrap.  The
 * central contract here is {@link org.hibernate.boot.model.type.spi.TypeInformation}
 * and its registry : {@link org.hibernate.boot.model.type.spi.TypeInformationAliasRegistry}
 * <p/>
 * Ultimately would like to move to a "producer" paradigm where we create
 * a Type "producer" and store that into the mapping model; as opposed to the
 * current "information" paradigm.
 */
package org.hibernate.boot.model.type.spi;
