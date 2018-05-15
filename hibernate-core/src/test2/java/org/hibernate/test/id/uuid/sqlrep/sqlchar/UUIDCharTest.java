/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.uuid.sqlrep.sqlchar;

import org.hibernate.cfg.Configuration;
import org.hibernate.type.UUIDCharType;

import org.hibernate.test.id.uuid.sqlrep.sqlbinary.UUIDBinaryTest;

/**
 * @author Steve Ebersole
 */
public class UUIDCharTest extends UUIDBinaryTest {
	@Override
	public void configure(Configuration cfg) {
		cfg.registerTypeOverride(
				new UUIDCharType() {
					@Override
					protected boolean registerUnderJavaType() {
						return true;
					}
				}
		);
	}

	@Override
	public String[] getMappings() {
		return new String[] { "id/uuid/sqlrep/Node2.hbm.xml" };
	}
}
