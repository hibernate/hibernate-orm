/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * A SQL dialect suitable for use with Sybase 11.9.2
 * (specifically: avoids ANSI JOIN syntax)
 *
 * @author Colm O' Flaherty
 * @deprecated use {@link SybaseASEDialect} instead
 */
@Deprecated
public class Sybase11Dialect extends SybaseASEDialect {}
