/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

/**
 * Optional {@link Type} contract for implementations that are aware of
 * how to extract values from stored procedure OUT/INOUT parameters.
 *
 * @author Steve Ebersole
 */
public interface ProcedureParameterExtractionAware<T>
		extends BindableType<T>, OutputableType<T> {
}
