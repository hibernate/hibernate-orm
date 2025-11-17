/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface SingularAttributeSourceOneToOne extends SingularAttributeSourceToOne {
	List<DerivedValueSource> getFormulaSources();

	boolean isConstrained();
}
