/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bugs.entity;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
@DiscriminatorValue("CHILD")
public class MultitenantChildEntity extends MultitenantParentEntity {
Long number;

@OneToMany(mappedBy = "child", fetch = FetchType.LAZY, orphanRemoval = true)
List<MultitenantReferenceEntity> references;
}
