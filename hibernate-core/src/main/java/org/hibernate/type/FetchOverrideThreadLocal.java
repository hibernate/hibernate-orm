/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.type;

import java.util.HashSet;
import java.util.Set;

public class FetchOverrideThreadLocal extends ThreadLocal<Boolean>{

	private static ThreadLocal<Set<Type>> tlocal = new ThreadLocal<Set<Type>>();
	public static void addFetchOverride(Type type)	{
		ensureSet();
		tlocal.get().add(type);
	}
	public static void removeFetchOverride(Type type)	{
		ensureSet();
		tlocal.get().remove(type);
	}

	public static boolean hasFetchOverride(Type t)	{
		ensureSet();
		return tlocal.get().contains(t);
	}
	private static void ensureSet()	{
		if (tlocal.get() == null) {
			tlocal.set(new HashSet<Type>());
		}
	}
}
