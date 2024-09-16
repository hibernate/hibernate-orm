/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AssociationOverrides({
@AssociationOverride(name = "from", joinColumns = @JoinColumn(name = "from2", nullable = false)),
@AssociationOverride(name = "to", joinColumns = @JoinColumn(name = "to2", nullable = false))
		})
public class Trip extends Move {
}
