/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;


import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;

/**
 * @author Steve Ebersole
 */
public class LongHolderTest extends AbstractHolderTest {
	protected IntegralDataTypeHolder makeHolder() {
		return IdentifierGeneratorHelper.getIntegralDataTypeHolder( Long.class );
	}
}
