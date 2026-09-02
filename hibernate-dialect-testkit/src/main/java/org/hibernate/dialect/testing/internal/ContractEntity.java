/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/// Fixed mapping model used by every Dialect contract profile.
///
/// @author Steve Ebersole
@Entity(name = "ContractEntity")
@Table(name = "dialect_contract_entity")
public class ContractEntity {
	@Id
	private Long id;

	private String name;

	private int quantity;
}
