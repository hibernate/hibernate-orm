/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.constraint;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Brett Meyer
 */
@DomainModel(
		annotatedClasses = {
				ConstraintTest.DataPoint.class,
				ConstraintTest.DataPoint2.class
		}
)
@SessionFactory
public class ConstraintTest {

	private static final int MAX_NAME_LENGTH = 30;

	private static final String EXPLICIT_FK_NAME_NATIVE = "fk_explicit_native";

	private static final String EXPLICIT_FK_NAME_JPA = "fk_explicit_jpa";

	private static final String EXPLICIT_UK_NAME = "uk_explicit";

	@Test
	@JiraKey(value = "HHH-7797")
	public void testUniqueConstraints(SessionFactoryScope scope) {
		MetadataImplementor metadata = scope.getMetadataImplementor();
		Column column = (Column) metadata.getEntityBinding( DataPoint.class.getName() )
				.getProperty( "foo1" ).getSelectables().get( 0 );
		assertThat( column.isNullable() ).isFalse();
		assertThat( column.isUnique() ).isTrue();

		column = (Column) metadata.getEntityBinding( DataPoint.class.getName() )
				.getProperty( "foo2" ).getSelectables().get( 0 );
		assertThat( column.isNullable() ).isTrue();
		assertThat( column.isUnique() ).isTrue();

		column = (Column) metadata.getEntityBinding( DataPoint.class.getName() )
				.getProperty( "id" ).getSelectables().get( 0 );
		assertThat( column.isNullable() ).isFalse();
		assertThat( column.isUnique() ).isTrue();
	}

	@Test
	@JiraKey(value = "HHH-1904")
	public void testConstraintNameLength(SessionFactoryScope scope) {
		MetadataImplementor metadata = scope.getMetadataImplementor();

		int foundCount = 0;
		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			for ( org.hibernate.mapping.Table table : namespace.getTables() ) {
				for ( ForeignKey fk : table.getForeignKeyCollection() ) {
					assertThat( fk.getName().length() ).isLessThanOrEqualTo( MAX_NAME_LENGTH );

					// ensure the randomly generated constraint name doesn't
					// happen if explicitly given
					Column column = fk.getColumn( 0 );
					if ( column.getName().equals( "explicit_native" ) ) {
						foundCount++;
						assertThat( fk.getName() ).isEqualTo( EXPLICIT_FK_NAME_NATIVE );
					}
					else if ( column.getName().equals( "explicit_jpa" ) ) {
						foundCount++;
						assertThat( fk.getName() ).isEqualTo( EXPLICIT_FK_NAME_JPA );
					}
				}

				for ( UniqueKey uk : table.getUniqueKeys().values() ) {
					assertThat( uk.getName().length() ).isLessThanOrEqualTo( MAX_NAME_LENGTH );

					// ensure the randomly generated constraint name doesn't
					// happen if explicitly given
					Column column = uk.getColumn( 0 );
					if ( column.getName().equals( "explicit" ) ) {
						foundCount++;
						assertThat( uk.getName() ).isEqualTo( EXPLICIT_UK_NAME );
					}
				}
			}

		}

		assertThat( foundCount )
				.describedAs( "Could not find the necessary columns." )
				.isEqualTo( 3 );
	}

	@Entity
	@Table(name = "DataPoint", uniqueConstraints = {
			@UniqueConstraint(name = EXPLICIT_UK_NAME, columnNames = {"explicit"})
	})
	public static class DataPoint {
		@Id
		@GeneratedValue
		@jakarta.persistence.Column(nullable = false, unique = true)
		public long id;

		@jakarta.persistence.Column(nullable = false, unique = true)
		public String foo1;

		@jakarta.persistence.Column(unique = true)
		public String foo2;

		public String explicit;
	}

	@Entity
	@Table(name = "DataPoint2")
	public static class DataPoint2 {
		@Id
		@GeneratedValue
		public long id;

		@OneToOne
		public DataPoint dp;

		@OneToOne
		@JoinColumn(name = "explicit_native",
				foreignKey = @jakarta.persistence.ForeignKey(name = EXPLICIT_FK_NAME_NATIVE))
		public DataPoint explicit_native;

		@OneToOne
		@JoinColumn(name = "explicit_jpa", foreignKey = @jakarta.persistence.ForeignKey(name = EXPLICIT_FK_NAME_JPA))
		public DataPoint explicit_jpa;
	}
}
