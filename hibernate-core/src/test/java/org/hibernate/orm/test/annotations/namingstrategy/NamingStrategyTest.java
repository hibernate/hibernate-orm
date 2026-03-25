package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.internal.AuditHelper;
import org.hibernate.boot.model.internal.ColumnNamingContexts;
import org.hibernate.boot.model.internal.TemporalHelper;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.StateManagementSettings.AUDIT_STRATEGY;

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
		assertThat( auditedEntityBinding.getAuxiliaryColumn( AuditHelper.CHANGESET_ID ).getName() )
				.isEqualTo( "AuditedEntity_txn_id" );
		assertThat( auditedEntityBinding.getAuxiliaryColumn( AuditHelper.MODIFICATION_TYPE ).getName() )
				.isEqualTo( "AuditedEntity_mod_type" );

		final Collection tagsBinding = metadata.getCollectionBinding( ContextEntity.class.getName() + ".tags" );
		assertThat( tagsBinding.getAuxiliaryColumn( AuditHelper.CHANGESET_ID ).getName() )
				.isEqualTo( "ContextEntity_REV" );
		assertThat( tagsBinding.getAuxiliaryColumn( AuditHelper.MODIFICATION_TYPE ).getName() )
				.isEqualTo( "ContextEntity_REVTYPE" );
	}

	@Test
	public void testUnknownColumnNamingContext() {
		assertThat( ColumnNamingContexts.forPropertyHolder( null ) ).isSameAs( ColumnNamingContexts.UNKNOWN );
		assertThat( ColumnNamingContexts.forColumns( null ) ).isSameAs( ColumnNamingContexts.UNKNOWN );
		assertThat( ColumnNamingContexts.forModel( new Object() ) ).isSameAs( ColumnNamingContexts.UNKNOWN );
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

	@Test
	public void testColumnNamingContextForEmbeddableAndElementCollection() {
		final Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( ContextEntity.class )
				.addAnnotatedClass( ContextParent.class )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new ContextNamingStrategy() )
				.build();

		final PersistentClass entityBinding = metadata.getEntityBinding( ContextEntity.class.getName() );
		final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
		assertThat( address.getProperty( "street" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "ContextEntity_street" );

		final Collection tags = metadata.getCollectionBinding( ContextEntity.class.getName() + ".tags" );
		assertThat( tags.getKey().getColumns().get( 0 ).getName() )
				.isEqualTo( "ContextEntity_owner_fk" );
		assertThat( tags.getElement().getColumns().get( 0 ).getName() )
				.isEqualTo( "ContextEntity_tag_value" );
	}

	@Test
	public void testColumnNamingContextForSubclassAndOneToManyAuditTables() {
		final ServiceRegistry validityRegistry =
				ServiceRegistryBuilder.buildServiceRegistry( Map.<String, Object>of( AUDIT_STRATEGY, "VALIDITY" ) );
		try {
			final Metadata metadata = new MetadataSources( validityRegistry )
					.addAnnotatedClass( AuditedJoinedParent.class )
					.addAnnotatedClass( AuditedJoinedChild.class )
					.addAnnotatedClass( AuditedItem.class )
					.getMetadataBuilder()
					.applyPhysicalNamingStrategy( new ContextNamingStrategy() )
					.build();

			final PersistentClass parent = metadata.getEntityBinding( AuditedJoinedParent.class.getName() );
			assertThat( parent.getAuxiliaryColumn( AuditHelper.INVALIDATING_CHANGESET_ID ).getName() )
					.isEqualTo( "AuditedJoinedParent_REVEND" );

			final PersistentClass child = metadata.getEntityBinding( AuditedJoinedChild.class.getName() );
			assertThat( child.getAuxiliaryColumn( AuditHelper.CHANGESET_ID ).getName() )
					.isEqualTo( "AuditedJoinedChild_REV" );

			final Collection items = metadata.getCollectionBinding( AuditedJoinedParent.class.getName() + ".items" );
			assertThat( items.getAuxiliaryColumn( AuditHelper.CHANGESET_ID ).getName() )
					.isEqualTo( "AuditedJoinedParent_REV" );
			assertThat( items.getAuxiliaryColumn( AuditHelper.MODIFICATION_TYPE ).getName() )
					.isEqualTo( "AuditedJoinedParent_REVTYPE" );
			assertThat( items.getAuxiliaryColumn( AuditHelper.INVALIDATING_CHANGESET_ID ).getName() )
					.isEqualTo( "AuditedJoinedParent_REVEND" );
		}
		finally {
			ServiceRegistryBuilder.destroy( validityRegistry );
		}
	}
}
