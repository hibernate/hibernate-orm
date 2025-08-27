/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor.merge;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

import java.util.ArrayList;
import java.util.List;

public class MergeAuditingInterceptor implements Interceptor {

	static final List<String> auditTrail = new ArrayList<>();

	@Override
	public void postMerge(Object source, Object target, Object id, Object[] targetState, Object[] originalState, String[] propertyNames, Type[] propertyTypes) {
		for ( int i = 0; i < propertyNames.length; i++ ) {
			if ( !propertyTypes[i].isEqual( originalState[i], targetState[i] ) ) {
				auditTrail.add( propertyNames[i] + " changed from " + originalState[i] + " to " + targetState[i] + " for " + id );
			}
		}
	}
}
