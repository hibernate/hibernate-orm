/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import java.lang.reflect.Proxy;
import java.util.Locale;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.internal.AuditHelper;
import org.hibernate.boot.model.internal.PropertyHolder;
import org.hibernate.boot.model.internal.TemporalHelper;
import org.hibernate.boot.model.naming.ColumnNamingContext;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test harness for ANN-716.
 *
 * @author Hardy Ferentschik
 */
@BaseUnitTest
public class NamingStrategyTest {

	private ServiceRegistry serviceRegistry;

	@BeforeEach
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testWithCustomNamingStrategy() {
		new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Person.class )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new DummyNamingStrategy() )
				.build();
	}

	@Test
	public void testWithUpperCaseNamingStrategy() throws Exception {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( A.class )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new PhysicalNamingStrategyStandardImpl() {
					@Override
					public Identifier toPhysicalColumnName(
							Identifier logicalName, JdbcEnvironment context) {
						return new Identifier( logicalName.getText().toUpperCase(), logicalName.isQuoted() );
					}
				} )
				.build();

		PersistentClass entityBinding = metadata.getEntityBinding( A.class.getName() );
		assertThat( entityBinding.getProperty( "name" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "NAME" );
		assertThat( entityBinding.getProperty( "value" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "VALUE" );
	}

	@Test
	public void testWithJpaCompliantNamingStrategy() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( A.class )
				.addAnnotatedClass( AddressEntry.class )
				.getMetadataBuilder()
				.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE )
				.build();

		Collection collectionBinding = metadata.getCollectionBinding( A.class.getName() + ".address" );
		assertThat( collectionBinding.getCollectionTable().getQuotedName().toUpperCase( Locale.ROOT ) )
				.describedAs(
						"Expecting A#address collection table name (implicit) to be [A_address] per JPA spec (section 11.1.8)"
				)
				.isEqualTo( "A_ADDRESS" );
	}

	@Test
	public void testWithoutCustomNamingStrategy() {
		new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Person.class )
				.buildMetadata();
	}

	@Test
	public void testColumnNamingContextForAnnotationsAndGeneratedColumns() {
		final Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( ContextEntity.class )
				.addAnnotatedClass( ContextParent.class )
				.addAnnotatedClass( SecondaryTableEntity.class )
				.addAnnotatedClass( TemporalEntity.class )
				.addAnnotatedClass( AuditedEntity.class )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new ContextNamingStrategy() )
				.build();

		final PersistentClass contextEntityBinding = metadata.getEntityBinding( ContextEntity.class.getName() );
		assertThat( contextEntityBinding.getProperty( "basicValue" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "ContextEntity_basicValue" );
		assertThat( contextEntityBinding.getProperty( "parent" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "ContextEntity_parent_id" );
		assertThat( ( (RootClass) contextEntityBinding ).getSoftDeleteColumn().getName() )
				.isEqualTo( "ContextEntity_gone" );

		final RootClass temporalEntityBinding = (RootClass) metadata.getEntityBinding( TemporalEntity.class.getName() );
		assertThat( temporalEntityBinding.getAuxiliaryColumn( TemporalHelper.ROW_START ).getName() )
				.isEqualTo( "TemporalEntity_valid_from" );
		assertThat( temporalEntityBinding.getAuxiliaryColumn( TemporalHelper.ROW_END ).getName() )
				.isEqualTo( "TemporalEntity_valid_to" );

		final RootClass auditedEntityBinding = (RootClass) metadata.getEntityBinding( AuditedEntity.class.getName() );
		assertThat( auditedEntityBinding.getAuxiliaryColumn( AuditHelper.TRANSACTION_ID ).getName() )
				.isEqualTo( "AuditedEntity_txn_id" );
		assertThat( auditedEntityBinding.getAuxiliaryColumn( AuditHelper.MODIFICATION_TYPE ).getName() )
				.isEqualTo( "AuditedEntity_mod_type" );

		final Collection tagsBinding = metadata.getCollectionBinding( ContextEntity.class.getName() + ".tags" );
		assertThat( tagsBinding.getAuxiliaryColumn( AuditHelper.TRANSACTION_ID ).getName() )
				.isEqualTo( "ContextEntity_tags_txn" );
		assertThat( tagsBinding.getAuxiliaryColumn( AuditHelper.MODIFICATION_TYPE ).getName() )
				.isEqualTo( "ContextEntity_tags_mod" );
	}

	@Test
	public void testPropertyHolderColumnNamingContextConstructor() {
		final ColumnNamingContext nullContext = new ColumnNamingContext( (PropertyHolder) null );
		assertThat( nullContext.entityName() ).isNull();
		assertThat( nullContext.className() ).isNull();

		final ColumnNamingContext propertyHolderContext =
				new ColumnNamingContext( propertyHolder( "ContextEntity", ContextEntity.class.getName() ) );
		assertThat( propertyHolderContext.entityName() ).isEqualTo( "ContextEntity" );
		assertThat( propertyHolderContext.className() ).isEqualTo( ContextEntity.class.getName() );
	}

	@Test
	public void testColumnNamingContextForSecondaryTablesWithoutPropertyHolder() {
		final Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( SecondaryTableEntity.class )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new ContextNamingStrategy() )
				.build();

		final PersistentClass entityBinding = metadata.getEntityBinding( SecondaryTableEntity.class.getName() );
		assertThat( entityBinding.getProperty( "secondaryValue" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "SecondaryTableEntity_secondaryValue" );
	}

	private static PropertyHolder propertyHolder(String entityName, String className) {
		return (PropertyHolder) Proxy.newProxyInstance(
				NamingStrategyTest.class.getClassLoader(),
				new Class<?>[] { PropertyHolder.class },
				(proxy, method, args) -> switch ( method.getName() ) {
					case "equals" -> args != null && proxy == args[0];
					case "getEntityName" -> entityName;
					case "getEntityOwnerClassName" -> className;
					case "hashCode" -> System.identityHashCode( proxy );
					case "toString" -> "PropertyHolder[" + entityName + ']';
					default -> defaultValue( method.getReturnType() );
				}
		);
	}

	private static Object defaultValue(Class<?> returnType) {
		return switch ( returnType.getName() ) {
			case "boolean" -> false;
			case "byte" -> (byte) 0;
			case "char" -> (char) 0;
			case "double" -> 0d;
			case "float" -> 0f;
			case "int" -> 0;
			case "long" -> 0L;
			case "short" -> (short) 0;
			default -> null;
		};
	}

}
