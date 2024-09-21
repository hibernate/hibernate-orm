/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import jakarta.persistence.Entity;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@RevisionEntity
public class InheritedRevEntity extends SequenceIdRevisionEntity {
}
