/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
