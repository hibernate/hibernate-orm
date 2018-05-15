/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * Standard implementation of JdbcParameterBindings
 *
 * @author Steve Ebersole
 */
public class JdbcParameterBindingsImpl implements JdbcParameterBindings {
	/**
	 * Singleton access
	 */
	public static final JdbcParameterBindingsImpl NO_BINDINGS = new JdbcParameterBindingsImpl();

	private Map<JdbcParameter, JdbcParameterBinding> bindingMap;

	@Override
	public void addBinding(JdbcParameter parameter, JdbcParameterBinding binding) {
		if ( bindingMap == null ) {
			bindingMap = new HashMap<>();
		}

		bindingMap.put( parameter, binding );
	}

	@Override
	public Collection<JdbcParameterBinding> getBindings() {
		return bindingMap == null ? Collections.emptyList() : bindingMap.values();
	}

	@Override
	public JdbcParameterBinding getBinding(JdbcParameter parameter) {
		if ( bindingMap == null ) {
			// no bindings
			return null;
		}
		return bindingMap.get( parameter );
	}

	@Override
	public void visitBindings(BiConsumer<JdbcParameter, JdbcParameterBinding> action) {
		for ( Map.Entry<JdbcParameter, JdbcParameterBinding> entry : bindingMap.entrySet() ) {
			action.accept( entry.getKey(), entry.getValue() );
		}
	}

	public void clear() {
		if ( bindingMap != null ) {
			bindingMap.clear();
		}
	}
}
