/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.mapping;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = {
				ColumnWithExplicitReferenceToPrimaryTableTest.AnEntity.class
		},
		integrationSettings = { @Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop") }
)
public class ColumnWithExplicitReferenceToPrimaryTableTest {
	@Test
	@JiraKey( value = "HHH-8539" )
	public void testColumnAnnotationWithExplicitReferenceToPrimaryTable(EntityManagerFactoryScope scope) {
		Assertions.assertNotNull( scope.getEntityManagerFactory() );
		scope.getEntityManagerFactory().close();
	}

	@Entity
	@Table( name = "THE_TABLE" )
	public static class AnEntity {
		@Id
		public Integer id;
		@Column( name = "THE_COLUMN", table = "THE_TABLE" )
		public String theValue;
	}
}
