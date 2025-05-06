/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.uk;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.orm.test.hbm.index.JournalingSchemaToolingTarget;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = JdbcSettings.DIALECT,
				provider = UniqueDelegateTest.MyDialectConfigurer.class
		)
)
@DomainModel( xmlMappings = "org/hibernate/orm/test/hbm/uk/person_unique.hbm.xml" )
@RequiresDialect(
		value = H2Dialect.class,
		comment = "Even though we use specialized Dialect, we still have calls happening to the "
				+ "underlying driver which will blow up on various underlying drivers.  Nothing here is "
				+ "Dialect-specific anyway, besides what the specialized Dialect exposes."
)
public class UniqueDelegateTest {
	private static int getColumnDefinitionUniquenessFragmentCallCount = 0;
	private static int getTableCreationUniqueConstraintsFragmentCallCount = 0;
	private static int getAlterTableToAddUniqueKeyCommandCallCount = 0;
	private static int getAlterTableToDropUniqueKeyCommandCallCount = 0;

	@Test
	@JiraKey( value = "HHH-10203" )
	public void testUniqueDelegateConsulted(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		final StandardServiceRegistry ssr = registryScope.getRegistry();
		final MetadataImplementor domainModel = modelScope.getDomainModel();

		final JournalingSchemaToolingTarget target = new JournalingSchemaToolingTarget();
		new SchemaCreatorImpl( ssr ).doCreation( domainModel, false, target );

		assertThat( getAlterTableToAddUniqueKeyCommandCallCount, equalTo( 1 ) );
		assertThat( getColumnDefinitionUniquenessFragmentCallCount, equalTo( 1 ) );
		assertThat( getTableCreationUniqueConstraintsFragmentCallCount, equalTo( 1 ) );

		new SchemaDropperImpl( ssr ).doDrop( domainModel, false, target );

		// unique keys are not dropped explicitly
		assertThat( getAlterTableToAddUniqueKeyCommandCallCount, equalTo( 1 ) );
		assertThat( getColumnDefinitionUniquenessFragmentCallCount, equalTo( 1 ) );
		assertThat( getTableCreationUniqueConstraintsFragmentCallCount, equalTo( 1 ) );
	}

	public static class MyDialect extends H2Dialect {
		private MyUniqueDelegate myUniqueDelegate;

		public MyDialect() {
			this.myUniqueDelegate = new MyUniqueDelegate( this );
		}

		@Override
		public UniqueDelegate getUniqueDelegate() {
			return myUniqueDelegate;
		}
	}

	public static class MyDialectConfigurer implements SettingProvider.Provider<Object> {
		@Override
		public Object getSetting() {
			return MyDialect.class;
		}
	}

	public static class MyUniqueDelegate extends AlterTableUniqueDelegate {

		/**
		 * Constructs DefaultUniqueDelegate
		 *
		 * @param dialect The dialect for which we are handling unique constraints
		 */
		public MyUniqueDelegate(Dialect dialect) {
			super( dialect );
		}

		@Override
		public String getColumnDefinitionUniquenessFragment(Column column,
				SqlStringGenerationContext context) {
			getColumnDefinitionUniquenessFragmentCallCount++;
			return super.getColumnDefinitionUniquenessFragment( column, context );
		}

		@Override
		public String getTableCreationUniqueConstraintsFragment(Table table,
				SqlStringGenerationContext context) {
			getTableCreationUniqueConstraintsFragmentCallCount++;
			return super.getTableCreationUniqueConstraintsFragment( table, context );
		}

		@Override
		public String getAlterTableToAddUniqueKeyCommand(
				UniqueKey uniqueKey, Metadata metadata,
				SqlStringGenerationContext context) {
			getAlterTableToAddUniqueKeyCommandCallCount++;
			return super.getAlterTableToAddUniqueKeyCommand( uniqueKey, metadata, context );
		}

		@Override
		public String getAlterTableToDropUniqueKeyCommand(
				UniqueKey uniqueKey, Metadata metadata,
				SqlStringGenerationContext context) {
			getAlterTableToDropUniqueKeyCommandCallCount++;
			return super.getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context );
		}
	}
}
