/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import jakarta.persistence.Entity;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.enhanced.SequenceIdRevisionMapping;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@RevisionEntity
public class InheritedRevEntity extends SequenceIdRevisionMapping {
}
