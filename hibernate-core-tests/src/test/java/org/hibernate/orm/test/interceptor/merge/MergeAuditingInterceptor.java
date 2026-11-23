package org.hibernate.orm.test.interceptor.merge;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

import java.util.ArrayList;
import java.util.List;

public class MergeAuditingInterceptor implements Interceptor {

	static final List<String> auditTrail = new ArrayList<>();

	@Override
	public void postMerge(
			@Nonnull Object source,
			@Nonnull Object target,
			@Nullable Object id,
			@Nonnull Object[] targetState,
			@Nullable Object[] originalState,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] propertyTypes) {
		for ( int i = 0; i < propertyNames.length; i++ ) {
			if ( !propertyTypes[i].isEqual( originalState[i], targetState[i] ) ) {
				auditTrail.add( propertyNames[i] + " changed from " + originalState[i] + " to " + targetState[i] + " for " + id );
			}
		}
	}
}
