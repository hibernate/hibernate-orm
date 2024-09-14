/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.transform;

/**
 * @author Steve Ebersole
 */
public class ColumnDefaultsBasicImpl implements ColumnDefaults {
	/**
	 * Singleton access
	 */
	public static final ColumnDefaultsBasicImpl INSTANCE = new ColumnDefaultsBasicImpl();

	@Override
	public Boolean isNullable() {
		return Boolean.TRUE;
	}

	@Override
	public Integer getLength() {
		return null;
	}

	@Override
	public Integer getScale() {
		return null;
	}

	@Override
	public Integer getPrecision() {
		return null;
	}

	@Override
	public Boolean isUnique() {
		return Boolean.FALSE;
	}

	@Override
	public Boolean isInsertable() {
		return Boolean.TRUE;
	}

	@Override
	public Boolean isUpdateable() {
		return Boolean.TRUE;
	}
}
