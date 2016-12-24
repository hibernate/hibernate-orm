/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression.domain;

import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.from.ColumnBinding;

/**
 * @author Steve Ebersole
 */
public class ColumnBindingGroupEmptyImpl implements ColumnBindingGroup {
	/**
	 * Singleton access
	 */
	public static final ColumnBindingGroupEmptyImpl INSTANCE = new ColumnBindingGroupEmptyImpl();

	@Override
	public List<ColumnBinding> getColumnBindings() {
		return Collections.emptyList();
	}
}
