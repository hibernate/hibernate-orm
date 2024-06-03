/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.community.dialect;

import org.hibernate.community.dialect.pagination.IngresLimitHandler;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.orm.test.dialect.AbstractLimitHandlerTest;

/**
 * @author Yanming Zhou
 */
public class IngresLimitHandlerTest extends AbstractLimitHandlerTest {

	@Override
	protected AbstractLimitHandler getLimitHandler() {
		return IngresLimitHandler.INSTANCE;
	}
}
