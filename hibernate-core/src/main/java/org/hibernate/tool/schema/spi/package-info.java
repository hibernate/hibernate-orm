/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * An SPI for tooling related to DDL generation, export, migration, and validation.
 * Schema management actions may be requested programmatically by calling
 * {@link org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator#process}.
 *
 * @see org.hibernate.tool.schema.spi.SchemaManagementTool
 * @see org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator
 */
package org.hibernate.tool.schema.spi;
