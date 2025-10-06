/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;


import jakarta.persistence.FindOption;

/**
 * Simple marker interface for FindOptions which can be applied to multiple id loading.
 */
public interface MultiFindOption extends FindOption {
}
