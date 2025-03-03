/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.HANADialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( value = HANADialect.class )
public class HANANoColumnInsertTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] {
				"org/hibernate/orm/test/ops/Competition.hbm.xml"
		};
	}

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		SessionFactoryImplementor sessionFactoryImplementor = null;
		try {
			sessionFactoryImplementor = super.produceSessionFactory( model );

			fail( "Should have thrown MappingException!" );
			return sessionFactoryImplementor;
		}
		catch (MappingException e) {
			assertThat( e.getMessage() ).startsWith(
					"The INSERT statement for table [Competition] contains no column, and this is not supported by [" + getDialect().getClass()
							.getName() );
		}
		return sessionFactoryImplementor;
	}

	@Test
	public void test() {
		sessionFactory();
	}
}
