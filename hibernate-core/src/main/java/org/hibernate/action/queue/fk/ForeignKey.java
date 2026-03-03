/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.fk;

import java.io.Serializable;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public record ForeignKey(
		String keyTable,
		String targetTable,
		List<String> keyColumns,
		List<String> targetColumns,
		boolean nullable,
		boolean deferrable) implements Serializable {
}
