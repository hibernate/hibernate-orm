/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.annotation;

import java.util.UUID;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.generator.Generator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.util.uuid.IdGeneratorCreationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {TheEntity.class, TheOtherEntity.class, AnotherEntity.class} )
@SessionFactory
@SkipForDialect( dialectClass = SybaseDialect.class, matchSubTypes = true,
		reason = "Skipped for Sybase to avoid problems with UUIDs potentially ending with a trailing 0 byte")
public class UuidGeneratorAnnotationTests {
	@Test
	public void verifyRandomUuidGeneratorModel(DomainModelScope scope) {
		scope.withHierarchy( TheEntity.class, (descriptor) -> {
			final Property idProperty = descriptor.getIdentifierProperty();
			final BasicValue value = (BasicValue) idProperty.getValue();

			assertThat( value.getCustomIdGeneratorCreator() ).isNotNull();
			final Generator generator = value.getCustomIdGeneratorCreator().createGenerator( new IdGeneratorCreationContext(
					scope.getDomainModel(),
					descriptor
			));

			assertThat( generator ).isInstanceOf( UuidGenerator.class );
			final UuidGenerator uuidGenerator = (UuidGenerator) generator;
			assertThat( uuidGenerator.getValueGenerator() ).isInstanceOf( StandardRandomStrategy.class );
		} );
	}

	@Test
	public void verifyCustomUuidGeneratorModel(DomainModelScope scope) {
		scope.withHierarchy( AnotherEntity.class, (descriptor) -> {
			final Property idProperty = descriptor.getIdentifierProperty();
			final BasicValue value = (BasicValue) idProperty.getValue();

			assertThat( value.getCustomIdGeneratorCreator() ).isNotNull();
			final Generator generator = value.getCustomIdGeneratorCreator().createGenerator( new IdGeneratorCreationContext(
					scope.getDomainModel(),
					descriptor
			));

			assertThat( generator ).isInstanceOf( UuidGenerator.class );
			final UuidGenerator uuidGenerator = (UuidGenerator) generator;
			assertThat( uuidGenerator.getValueGenerator() ).isInstanceOf( CustomUuidValueGenerator.class );
		} );
	}

	@Test
	public void basicUseTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			TheEntity steve = new TheEntity("steve");
			session.persist( steve );
			session.flush();
			assertThat( steve.id ).isNotNull();
		} );
	}

	@Test
	public void nonPkUseTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			TheOtherEntity gavin = new TheOtherEntity("gavin");
			session.persist( gavin );
			session.flush();
			assertThat( gavin.id ).isNotNull();
		} );
	}

	@Test
	void testCustomValueGenerator(SessionFactoryScope sessionFactoryScope) {
		sessionFactoryScope.inTransaction( (session) -> {
			final UUID sessionIdentifier = session.getSessionIdentifier();

			final AnotherEntity anotherEntity = new AnotherEntity( "johnny" );
			session.persist( anotherEntity );
			assertThat( anotherEntity.getId() ).isNotNull();

			session.flush();
			assertThat( anotherEntity.getId() ).isNotNull();
			assertThat( anotherEntity.getId().getMostSignificantBits() ).isEqualTo( sessionIdentifier.getMostSignificantBits() );
			assertThat( anotherEntity.getId().getLeastSignificantBits() ).isEqualTo( 1L );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
