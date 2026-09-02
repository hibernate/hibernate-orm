/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.spi;

/**
 * Commonality between the builders for results and fetches.
 *
 * @see ResultBuilder
 * @see FetchBuilder
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface GraphNodeBuilder {
}
