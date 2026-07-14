/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.reventity;

import jakarta.persistence.Entity;

import org.hibernate.envers.LongRevisionMapping;
import org.hibernate.envers.RevisionEntity;

/**
 * A revision entity using a {@code long} revision number via {@link LongRevisionMapping}.
 */
@Entity
@RevisionEntity
public class LongRevisionMappingRevEntity extends LongRevisionMapping {
}
