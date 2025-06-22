/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.enhanced;

import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.id.enhanced.AccessCallback;

class SourceMock implements AccessCallback {
	private final String tenantId;
	private final long initialValue;
	private final int increment;
	private volatile long currentValue;
	private volatile int timesCalled;

	public SourceMock(long initialValue) {
		this( initialValue, 1 );
	}

	public SourceMock(long initialValue, int increment) {
		this( null, initialValue, increment );
	}

	public SourceMock(String tenantId, long initialValue, int increment) {
		this( tenantId, initialValue, increment, 0 );
	}

	public SourceMock(long initialValue, int increment, int timesCalled) {
		this( null, initialValue, increment, timesCalled );
	}

	public SourceMock(String tenantId, long initialValue, int increment, int timesCalled) {
		this.tenantId = tenantId;
		this.increment = increment;
		this.timesCalled = timesCalled;
		if ( timesCalled != 0 ) {
			this.currentValue = initialValue;
			this.initialValue = 1;
		}
		else {
			this.currentValue = -1;
			this.initialValue = initialValue;
		}
	}

	@Override
	public synchronized IntegralDataTypeHolder getNextValue() {
		try {
			if ( timesCalled == 0 ) {
				currentValue = initialValue;
			}
			else {
				currentValue += increment;
			}
			IdentifierGeneratorHelper.BasicHolder result = new IdentifierGeneratorHelper.BasicHolder( Long.class );
			result.initialize( currentValue );
			return result;
		}
		finally {
			++timesCalled;
		}
	}

	@Override
	public String getTenantIdentifier() {
		return tenantId;
	}

	public int getTimesCalled() {
		return timesCalled;
	}

	public long getCurrentValue() {
		return currentValue;
	}
}
