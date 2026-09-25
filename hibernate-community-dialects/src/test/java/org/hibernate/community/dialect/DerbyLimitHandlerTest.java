package org.hibernate.community.dialect;

import org.hibernate.dialect.pagination.spi.AbstractLimitHandler;
import org.hibernate.community.dialect.pagination.DerbyLimitHandler;
import org.hibernate.orm.test.dialect.AbstractLimitHandlerTest;

/**
 * @author Yanming Zhou
 */
public class DerbyLimitHandlerTest extends AbstractLimitHandlerTest {

	@Override
	protected AbstractLimitHandler getLimitHandler() {
		return DerbyLimitHandler.INSTANCE;
	}
}
