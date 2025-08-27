/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.result.spi;

import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;

/**
 * @author Steve Ebersole
 */
public interface ResultContext {
	SharedSessionContractImplementor getSession();

	Set<String> getSynchronizedQuerySpaces();

	QueryOptions getQueryOptions();
}
