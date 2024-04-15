/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.enhanced;

import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;

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
