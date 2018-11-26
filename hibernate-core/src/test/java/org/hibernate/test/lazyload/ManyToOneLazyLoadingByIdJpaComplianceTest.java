/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.LazyInitializationException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.apache.xpath.operations.Bool;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class ManyToOneLazyLoadingByIdJpaComplianceTest extends ManyToOneLazyLoadingByIdTest {

	@Override
	protected void addMappings(Map settings) {
		settings.put( AvailableSettings.JPA_PROXY_COMPLIANCE, Boolean.TRUE );
	}

	protected void assertProxyState(Continent continent) {
		assertEquals( "Europe", continent.getName() );
	}
}
