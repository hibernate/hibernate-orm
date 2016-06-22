package org.hibernate.type;

import java.util.HashSet;
import java.util.Set;

public class FetchOverrideThreadLocal extends ThreadLocal<Boolean>{

	private static ThreadLocal<Set<Type>> tlocal = new ThreadLocal<Set<Type>>();
	public static void addFetchOverride(Type type)
	{
		ensureSet();
		tlocal.get().add(type);
	}
	public static void removeFetchOverride(Type type)
	{
		ensureSet();
		tlocal.get().remove(type);
	}

	public static boolean hasFetchOverride(Type t)
	{
		ensureSet();
		return tlocal.get().contains(t);
	}
	private static void ensureSet()
	{
		if (tlocal.get() == null)
			tlocal.set(new HashSet<Type>());
	}
}
