/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.dynamic;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class DynamicEntityTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] {
				"org/hibernate/userguide/mapping/dynamic/Book.hbm.xml"
		};
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		//tag::mapping-model-dynamic-setting-example[]
		settings.put( "hibernate.default_entity_mode", "dynamic-map" );
		//end::mapping-model-dynamic-setting-example[]
		return settings;
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::mapping-model-dynamic-example[]

			Map<String, String> book = new HashMap<>();
			book.put( "isbn", "978-9730228236" );
			book.put( "title", "High-Performance Java Persistence" );
			book.put( "author", "Vlad Mihalcea" );

			entityManager
				.unwrap(Session.class)
				.save( "Book", book );
			//end::mapping-model-dynamic-example[]
		} );
	}
}
