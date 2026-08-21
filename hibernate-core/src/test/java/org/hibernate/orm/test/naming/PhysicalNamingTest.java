/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.naming;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

@Jpa(annotatedClasses = PhysicalNamingTest.TheEntity.class,
	integrationSettings = @Setting(
			name = MappingSettings.PHYSICAL_NAMING_STRATEGY,
			value = "org.hibernate.orm.test.naming.PhysicalNamingTest$NamingStrategy"))
public class PhysicalNamingTest {

	public static class NamingStrategy
			extends CamelCaseToUnderscoresNamingStrategy {
		@Override
		protected Identifier unquotedIdentifier(Identifier name) {
			Identifier identifier = super.unquotedIdentifier( name );
			return new Identifier( identifier.getText() + "_",
					identifier.isQuoted() );
		}
	}


	@JiraKey("HHH-19515")
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// make sure _ was not appended more than once
			em.createNativeQuery(
							"select its_id_, its_name_, its_friend_its_id_ from the_entity_",
							Object[].class )
					.getResultList();
		});
	}

	@Entity(name = "TheEntity")
	static class TheEntity {
		@Id long itsId;
		String itsName;
		@ManyToOne
		TheEntity itsFriend;
	}
}
