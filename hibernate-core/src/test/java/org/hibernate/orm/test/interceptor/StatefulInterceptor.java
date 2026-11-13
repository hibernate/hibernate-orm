package org.hibernate.orm.test.interceptor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

public class StatefulInterceptor implements Interceptor {

	private final List<Log> logs = new ArrayList<>();

	@Override
	public boolean onPersist(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		if ( !(entity instanceof Log) ) {
			logs.add( new Log( "insert", (String) id, entity.getClass().getName() ) );
		}
		return false;
	}

	@Override
	public boolean onFlushDirty(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] currentState,
			@Nullable Object[] previousState,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		if ( !(entity instanceof Log) ) {
			logs.add( new Log( "update", (String) id, entity.getClass().getName() ) );
		}
		return false;
	}

	public List<Log> drainLogs() {
		final List<Log> collectedLogs = new ArrayList<>( logs );
		logs.clear();
		return collectedLogs;
	}
}
