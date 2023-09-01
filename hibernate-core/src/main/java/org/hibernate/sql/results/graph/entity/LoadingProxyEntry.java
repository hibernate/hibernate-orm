/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import java.util.Locale;

import org.hibernate.engine.spi.EntityKey;

public class LoadingProxyEntry {
	private final EntityInitializer entityInitializer;
	private final EntityKey entityKey;
	private final Object proxy;
	private final Object proxyTarget;

	public LoadingProxyEntry(
			EntityInitializer entityInitializer,
			EntityKey entityKey,
			Object proxy,
			Object proxyTarget) {
		this.entityInitializer = entityInitializer;
		this.entityKey = entityKey;
		this.proxy = proxy;
		this.proxyTarget = proxyTarget;
	}

	public EntityInitializer getEntityInitializer() {
		return entityInitializer;
	}

	public EntityKey getEntityKey() {
		return entityKey;
	}

	public Object getProxy() {
		return proxy;
	}

	public Object getProxyTarget() {
		return proxyTarget;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"LoadingProxyEntry(type=%s, id=%s)@%s",
				proxy.getClass(),
				getEntityKey().getIdentifier(),
				System.identityHashCode( this )
		);
	}
}
