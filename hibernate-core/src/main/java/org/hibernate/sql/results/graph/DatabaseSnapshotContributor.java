/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.TableGroup;

/**
 * Contract for model-parts which contribute to their container's
 * state array for database snapshots
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface DatabaseSnapshotContributor extends Fetchable {

	/**
	 * Create a DomainResult to be used when selecting snapshots from the database.
	 * <p>
	 * By default, simply use {@link #createDomainResult}
	 */
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	default <T> DomainResult<T> createSnapshotDomainResult(
			NavigablePath navigablePath,
			TableGroup parentTableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return createDomainResult( navigablePath, parentTableGroup, null, creationState );
	}
}
