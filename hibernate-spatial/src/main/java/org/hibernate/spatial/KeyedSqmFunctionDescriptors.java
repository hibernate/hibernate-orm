/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial;


import java.util.Map;

import org.hibernate.query.sqm.function.SqmFunctionDescriptor;

/**
 * Internal contract for a collection of SqmFunctionDescriptors, together with their key
 */
public interface KeyedSqmFunctionDescriptors {

	/**
	 * Return the SqmFunctionDescriptors as map of {@code FunctionKey} to {@code SqmFunctionDescriptor}
	 *
	 * @return the SqmFunctionDescriptors as map of {@code FunctionKey} to {@code SqmFunctionDescriptor}
	 */
	Map<FunctionKey, SqmFunctionDescriptor> asMap();

}
