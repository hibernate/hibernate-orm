/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.type.converter;

import java.sql.Types;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Nationalized;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the combination of @Nationalized and @Convert
 *
 * @author Steve Ebersole
 */
public class AndNationalizedTests extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-9599")
	public void basicTest() {
		Configuration cfg = new Configuration()
				.addAnnotatedClass( TestEntity.class );
		cfg.buildMappings();

		final PersistentClass entityBinding = cfg.getClassMapping( TestEntity.class.getName() );
		assertEquals(
				Types.NVARCHAR,
				entityBinding.getProperty( "name" ).getType().sqlTypes( cfg.buildMapping() )[0]
		);
	}

	@Entity(name = "TestEntity")
	@Table(name = "TestEntity")
	public static class TestEntity {
		@Id
		public Integer id;
		@Nationalized
		@Convert(converter = NameConverter.class)
		public Name name;
	}

	public static class Name {
		private final String text;

		public Name(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}

	public static class NameConverter implements AttributeConverter<Name,String> {
		@Override
		public String convertToDatabaseColumn(Name attribute) {
			return attribute.getText();
		}

		@Override
		public Name convertToEntityAttribute(String dbData) {
			return new Name( dbData );
		}
	}
}
