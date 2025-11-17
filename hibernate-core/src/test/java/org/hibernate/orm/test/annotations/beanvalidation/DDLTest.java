/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import org.hibernate.cfg.ValidationSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test verifying that DDL constraints get applied when Bean Validation / Hibernate Validator are enabled.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@ServiceRegistry(settings = {
		@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "DDL"),
		@Setting(name = PersistentTableStrategy.DROP_ID_TABLES, value = "true"),
		@Setting(name = GlobalTemporaryTableMutationStrategy.DROP_ID_TABLES, value = "true"),
		@Setting(name = LocalTemporaryTableMutationStrategy.DROP_ID_TABLES, value = "true")
})
@DomainModel(annotatedClasses = {
		Address.class,
		Tv.class,
		TvOwner.class,
		Rock.class
})
@SessionFactory
class DDLTest {

	@BeforeAll
	static void beforeAll(SessionFactoryScope scope) {
		// we want to get the SF built before we inspect the boot metamodel,
		// if we don't -- the integrators won't get applied, and hence DDL validation mode will not be applied either:
		scope.getSessionFactory();
	}

	@Test
	void testBasicDDL(SessionFactoryScope scope) {
		PersistentClass classMapping = scope.getMetadataImplementor().getEntityBinding( Address.class.getName() );
		Column stateColumn = classMapping.getProperty( "state" ).getColumns().get( 0 );
		assertThat( stateColumn.getLength() ).isEqualTo( 3L );
		Column zipColumn = classMapping.getProperty( "zip" ).getColumns().get( 0 );
		assertThat( zipColumn.getLength() ).isEqualTo( 5L );
		assertThat( zipColumn.isNullable() ).isFalse();
	}

	@Test
	void testNotNullDDL(SessionFactoryScope scope) {
		PersistentClass classMapping = scope.getMetadataImplementor().getEntityBinding( Address.class.getName() );
		Column stateColumn = classMapping.getProperty( "state" ).getColumns().get( 0 );
		assertThat( stateColumn.isNullable() )
				.as( "Validator annotations are applied on state as it is @NotNull" )
				.isFalse();

		Column line1Column = classMapping.getProperty( "line1" ).getColumns().get( 0 );
		assertThat( line1Column.isNullable() )
				.as( "Validator annotations are applied on line1 as it is @NotEmpty" )
				.isFalse();

		Column line2Column = classMapping.getProperty( "line2" ).getColumns().get( 0 );
		assertThat( line2Column.isNullable() )
				.as( "Validator annotations are applied on line2 as it is @NotBlank" )
				.isFalse();

		Column line3Column = classMapping.getProperty( "line3" ).getColumns().get( 0 );
		assertThat( line3Column.isNullable() )
				.as( "Validator composition of type OR should result in line3 being nullable" )
				.isTrue();

		Column line4Column = classMapping.getProperty( "line4" ).getColumns().get( 0 );
		assertThat( line4Column.isNullable() )
				.as( "Validator composition of type OR should result in line4 being not-null" )
				.isFalse();

		Column line5Column = classMapping.getProperty( "line5" ).getColumns().get( 0 );
		assertThat( line5Column.isNullable() )
				.as( "Validator composition of type AND should result in line5 being not-null" )
				.isFalse();

		Column line6Column = classMapping.getProperty( "line6" ).getColumns().get( 0 );
		assertThat( line6Column.isNullable() )
				.as( "Validator composition of type AND should result in line6 being not-null" )
				.isFalse();

		Column line7Column = classMapping.getProperty( "line7" ).getColumns().get( 0 );
		assertThat( line7Column.isNullable() )
				.as( "Validator composition of type OR should result in line7 being nullable" )
				.isTrue();

		Column line8Column = classMapping.getProperty( "line8" ).getColumns().get( 0 );
		assertThat( line8Column.isNullable() )
				.as( "Validator should result in line8 being not-null" )
				.isFalse();

		Column line9Column = classMapping.getProperty( "line9" ).getColumns().get( 0 );
		assertThat( line9Column.isNullable() ).as( "Validator should result in line9 being nullable" )
				.isTrue();
	}

	@Test
	void testApplyOnIdColumn(SessionFactoryScope scope) {
		PersistentClass classMapping = scope.getMetadataImplementor().getEntityBinding( Tv.class.getName() );
		Column serialColumn = classMapping.getIdentifierProperty().getColumns().get( 0 );
		assertThat( serialColumn.getLength() ).as( "Validator annotation not applied on ids" ).isEqualTo( 2L );
	}

	@Test
	void testLengthConstraint(SessionFactoryScope scope) {
		PersistentClass classMapping = scope.getMetadataImplementor().getEntityBinding( Tv.class.getName() );
		Column modelColumn = classMapping.getProperty( "model" ).getColumns().get( 0 );
		assertThat( modelColumn.getLength() ).isEqualTo( 5L );
	}

	@Test
	void testApplyOnManyToOne(SessionFactoryScope scope) {
		PersistentClass classMapping = scope.getMetadataImplementor().getEntityBinding( TvOwner.class.getName() );
		Column serialColumn = classMapping.getProperty( "tv" ).getColumns().get( 0 );
		assertThat( serialColumn.isNullable() )
				.as( "Validator annotations not applied on associations" )
				.isFalse();
	}

	@Test
	void testSingleTableAvoidNotNull(SessionFactoryScope scope) {
		PersistentClass classMapping = scope.getMetadataImplementor().getEntityBinding( Rock.class.getName() );
		Column serialColumn = classMapping.getProperty( "bit" ).getColumns().get( 0 );
		assertThat( serialColumn.isNullable() )
				.as( "Notnull should not be applied on single tables" )
				.isTrue();
	}

	@Test
	void testNotNullOnlyAppliedIfEmbeddedIsNotNullItself(SessionFactoryScope scope) {
		PersistentClass classMapping = scope.getMetadataImplementor().getEntityBinding( Tv.class.getName() );
		Property property = classMapping.getProperty( "tuner.frequency" );
		Column serialColumn = property.getColumns().get( 0 );
		assertThat( serialColumn.isNullable() )
				.as( "Validator annotations are applied on tuner as it is @NotNull" )
				.isFalse();

		property = classMapping.getProperty( "recorder.time" );
		serialColumn = property.getColumns().get( 0 );
		assertThat( serialColumn.isNullable() )
				.as( "Validator annotations are applied on tuner as it is @NotNull" )
				.isTrue();
	}
}
