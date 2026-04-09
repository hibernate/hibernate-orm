/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2022-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.orm.jbt.api.factory;


import java.io.File;
import java.lang.reflect.Field;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.List;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.tool.reveng.api.export.ArtifactCollector;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.ide.completion.HQLCodeAssist;
import org.hibernate.tool.reveng.ide.completion.HQLCompletionProposal;
import org.hibernate.tool.reveng.internal.export.ddl.DdlExporter;
import org.hibernate.tool.reveng.internal.export.hbm.Cfg2HbmTool;
import org.hibernate.tool.reveng.internal.export.hbm.HbmExporter;
import org.hibernate.tool.reveng.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.internal.reveng.strategy.DelegatingStrategy;
import org.hibernate.tool.reveng.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.reveng.internal.reveng.strategy.TableFilter;
import org.hibernate.tool.orm.jbt.api.wrp.ColumnWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.ConfigurationWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.DatabaseReaderWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.ExporterWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.HqlCodeAssistWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.HqlCompletionProposalWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.NamingStrategyWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.OverrideRepositoryWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.PersistentClassWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.PropertyWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.RevengSettingsWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.RevengStrategyWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.SchemaExportWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.TableFilterWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.TableWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.TypeFactoryWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.Wrapper;
import org.hibernate.tool.orm.jbt.internal.factory.ConfigurationWrapperFactory;
import org.hibernate.tool.orm.jbt.internal.factory.RevengStrategyWrapperFactory;
import org.hibernate.tool.orm.jbt.internal.factory.TableWrapperFactory;
import org.hibernate.tool.orm.jbt.internal.util.ConfigurationMetadataDescriptor;
import org.hibernate.tool.orm.jbt.internal.util.DummyMetadataBuildingContext;
import org.hibernate.tool.orm.jbt.internal.util.JpaConfiguration;
import org.hibernate.tool.orm.jbt.internal.util.MetadataHelper;
import org.hibernate.tool.orm.jbt.internal.util.NativeConfiguration;
import org.hibernate.tool.orm.jbt.internal.util.RevengConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WrapperFactoryTest {
	
	@Test
	public void testCreateArtifactCollectorWrapper() {
		Object artifactCollectorWrapper = WrapperFactory.createArtifactCollectorWrapper();
		assertNotNull(artifactCollectorWrapper);
		assertTrue(artifactCollectorWrapper instanceof Wrapper);
		Object wrappedArtifactCollector = ((Wrapper)artifactCollectorWrapper).getWrappedObject();
		assertTrue(wrappedArtifactCollector instanceof ArtifactCollector);
	}
	
	@Test
	public void testCreateCfg2HbmWrapper() {
		Object cfg2HbmWrapper = WrapperFactory.createCfg2HbmWrapper();
		assertNotNull(cfg2HbmWrapper);
		assertTrue(cfg2HbmWrapper instanceof Wrapper);
		Object cfg2HbmTool = ((Wrapper)cfg2HbmWrapper).getWrappedObject();
		assertTrue(cfg2HbmTool instanceof Cfg2HbmTool);
	}
	
	@Test
	public void testCreateNamingStrategyWrapper() {
		Object namingStrategyWrapper = WrapperFactory.createNamingStrategyWrapper(ImplicitNamingStrategyJpaCompliantImpl.class.getName());
		assertNotNull(namingStrategyWrapper);
		assertTrue(namingStrategyWrapper instanceof NamingStrategyWrapper);
		Object wrappedNamingStrategy = ((NamingStrategyWrapper)namingStrategyWrapper).getWrappedObject();
		assertTrue(wrappedNamingStrategy instanceof ImplicitNamingStrategyJpaCompliantImpl);
		namingStrategyWrapper = null;
		assertNull(namingStrategyWrapper);
		try {
			namingStrategyWrapper = WrapperFactory.createNamingStrategyWrapper("foo");
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Exception while looking up class 'foo'");
		}
		assertNull(namingStrategyWrapper);
	}
	
	@Test
	public void testCreateOverrideRepositoryWrapper() {
		Object overrideRepositoryWrapper = WrapperFactory.createOverrideRepositoryWrapper();
		assertNotNull(overrideRepositoryWrapper);
		assertTrue(overrideRepositoryWrapper instanceof OverrideRepositoryWrapper);
		Object wrappedOverrideRepository = ((Wrapper)overrideRepositoryWrapper).getWrappedObject();
		assertTrue(wrappedOverrideRepository instanceof OverrideRepository);
	}
	
	@Test
	public void testCreateRevengStrategyWrapper() throws Exception {
		Field delegateField = DelegatingStrategy.class.getDeclaredField("delegate");
		delegateField.setAccessible(true);
		Object reverseEngineeringStrategyWrapper = WrapperFactory
				.createRevengStrategyWrapper();
		assertNotNull(reverseEngineeringStrategyWrapper);
		assertTrue(reverseEngineeringStrategyWrapper instanceof Wrapper);
		assertTrue(((Wrapper)reverseEngineeringStrategyWrapper).getWrappedObject() instanceof DefaultStrategy);
		RevengStrategyWrapper delegate = (RevengStrategyWrapper)reverseEngineeringStrategyWrapper;
		reverseEngineeringStrategyWrapper = WrapperFactory
				.createRevengStrategyWrapper(
						TestDelegatingStrategy.class.getName(), 
						delegate);
		assertNotNull(reverseEngineeringStrategyWrapper);
		assertTrue(reverseEngineeringStrategyWrapper instanceof Wrapper);
		assertTrue(((Wrapper)reverseEngineeringStrategyWrapper).getWrappedObject() instanceof TestDelegatingStrategy);
		assertSame(
				delegateField.get(((Wrapper)reverseEngineeringStrategyWrapper).getWrappedObject()), 
				delegate.getWrappedObject());
	}
	
	@Test
	public void testCreateRevengSettingsWrapper() {
		Object reverseEngineeringSettingsWrapper = null;
		RevengStrategyWrapper strategy = RevengStrategyWrapperFactory.createRevengStrategyWrapper();
		reverseEngineeringSettingsWrapper = WrapperFactory.createRevengSettingsWrapper(strategy);
		assertNotNull(reverseEngineeringSettingsWrapper);
		assertTrue(reverseEngineeringSettingsWrapper instanceof RevengSettingsWrapper);
		RevengSettings revengSettings = (RevengSettings)((RevengSettingsWrapper)reverseEngineeringSettingsWrapper).getWrappedObject();
		assertSame(strategy.getWrappedObject(), revengSettings.getRootStrategy());
	}
	
	@Test
	public void testCreateNativeConfigurationWrapper() {
		Object configurationWrapper = WrapperFactory.createNativeConfigurationWrapper();
		assertNotNull(configurationWrapper);
		assertTrue(configurationWrapper instanceof ConfigurationWrapper);
		Object wrappedConfiguration = ((ConfigurationWrapper)configurationWrapper).getWrappedObject();
		assertTrue(wrappedConfiguration instanceof NativeConfiguration);
	}
		
	@Test
	public void testCreateRevengConfigurationWrapper() {
		Object configurationWrapper = WrapperFactory.createRevengConfigurationWrapper();
		assertNotNull(configurationWrapper);
		assertTrue(configurationWrapper instanceof ConfigurationWrapper);
		Object wrappedConfiguration = ((ConfigurationWrapper)configurationWrapper).getWrappedObject();
		assertTrue(wrappedConfiguration instanceof RevengConfiguration);
	}
		
	@Test
	public void testCreateJpaConfigurationWrapper() {
		Object configurationWrapper = WrapperFactory.createJpaConfigurationWrapper(null, null);
		assertNotNull(configurationWrapper);
		assertTrue(configurationWrapper instanceof ConfigurationWrapper);
		Object wrappedConfiguration = ((ConfigurationWrapper)configurationWrapper).getWrappedObject();
		assertTrue(wrappedConfiguration instanceof JpaConfiguration);
	}
	
	@Test
	public void testCreateColumnWrapper() {
		Object columnWrapper = WrapperFactory.createColumnWrapper(null);
		assertNotNull(columnWrapper);
		assertTrue(columnWrapper instanceof ColumnWrapper);
		Object wrappedColumn = ((ColumnWrapper)columnWrapper).getWrappedObject();
		assertTrue(wrappedColumn instanceof Column);
	}
	
	@Test
	public void testCreateRootClassWrapper() {
		Object rootClassWrapper = WrapperFactory.createRootClassWrapper();
		assertNotNull(rootClassWrapper);
		assertTrue(rootClassWrapper instanceof PersistentClassWrapper);
		assertTrue(((PersistentClassWrapper)rootClassWrapper).getWrappedObject() instanceof RootClass);
	}
	
	@Test
	public void testCreateSingleTableSubclassWrapper() {
		Object rootClassWrapper = WrapperFactory.createRootClassWrapper();
		Object singleTableSubclassWrapper = WrapperFactory.createSingleTableSubClassWrapper(
				rootClassWrapper);
		assertNotNull(singleTableSubclassWrapper);
		assertTrue(singleTableSubclassWrapper instanceof PersistentClassWrapper);
		PersistentClass persistentClass = (PersistentClass)((PersistentClassWrapper)singleTableSubclassWrapper).getWrappedObject();
		assertTrue(persistentClass instanceof SingleTableSubclass);
		assertSame(
				((SingleTableSubclass)persistentClass).getRootClass(), 
				((PersistentClassWrapper)rootClassWrapper).getWrappedObject());
	}
	
	@Test
	public void testCreateJoinedSubclassWrapper() {
		Object rootClassWrapper = WrapperFactory.createRootClassWrapper();
		Object joinedTableSubclassWrapper = WrapperFactory.createJoinedTableSubClassWrapper(
				rootClassWrapper);
		assertNotNull(joinedTableSubclassWrapper);
		assertTrue(joinedTableSubclassWrapper instanceof PersistentClassWrapper);
		PersistentClass persistentClass = (PersistentClass)((PersistentClassWrapper)joinedTableSubclassWrapper).getWrappedObject();
		assertTrue(persistentClass instanceof JoinedSubclass);
		assertSame(
				((JoinedSubclass)persistentClass).getRootClass(), 
				((PersistentClassWrapper)rootClassWrapper).getWrappedObject());
	}
	
	@Test
	public void testCreateSpecialRootClassWrapper() {
		PropertyWrapper propertyWrapper = (PropertyWrapper)WrapperFactory.createPropertyWrapper();
		Object specialRootClassWrapper = WrapperFactory.createSpecialRootClassWrapper(propertyWrapper);
		assertNotNull(specialRootClassWrapper);
        assertInstanceOf(PersistentClassWrapper.class, specialRootClassWrapper);
		assertSame(propertyWrapper, ((PersistentClassWrapper)specialRootClassWrapper).getProperty());
	}
	
	@Test
	public void testCreatePropertyWrapper() {
		Object propertyWrapper = WrapperFactory.createPropertyWrapper();
		assertNotNull(propertyWrapper);
		assertTrue(propertyWrapper instanceof PropertyWrapper);
	}
	
	@Test
	public void testCreateHqlCompletionProposalWrapper() {
		HQLCompletionProposal hqlCompletionProposalTarget = 
				new HQLCompletionProposal(HQLCompletionProposal.PROPERTY, Integer.MAX_VALUE);
		Object hqlCompletionProposalWrapper = 
				WrapperFactory.createHqlCompletionProposalWrapper(hqlCompletionProposalTarget);
		assertNotNull(hqlCompletionProposalWrapper);
		assertTrue(hqlCompletionProposalWrapper instanceof HqlCompletionProposalWrapper);
	}
		
	@Test
	public void testCreateArrayWrapper() {
		Object persistentClassWrapper = WrapperFactory.createRootClassWrapper();
		PersistentClass persistentClassTarget = (PersistentClass)((Wrapper)persistentClassWrapper).getWrappedObject();
		Object arrayWrapper = WrapperFactory.createArrayWrapper(persistentClassWrapper);
		Value wrappedArray = (Value)((Wrapper)arrayWrapper).getWrappedObject();
		assertTrue(wrappedArray instanceof Array);
		assertSame(((Array)wrappedArray).getOwner(), persistentClassTarget);
	}

	@Test
	public void testCreateBagWrapper() {
		Object persistentClassWrapper = WrapperFactory.createRootClassWrapper();
		PersistentClass persistentClassTarget = (PersistentClass)((Wrapper)persistentClassWrapper).getWrappedObject();
		Object bagWrapper = WrapperFactory.createBagWrapper(persistentClassWrapper);
		Value wrappedBag = (Value)((Wrapper)bagWrapper).getWrappedObject();
		assertTrue(wrappedBag instanceof Bag);
		assertSame(((Bag)wrappedBag).getOwner(), persistentClassTarget);
	}

	@Test
	public void testCreateListWrapper() {
		Object persistentClassWrapper = WrapperFactory.createRootClassWrapper();
		PersistentClass persistentClassTarget = (PersistentClass)((Wrapper)persistentClassWrapper).getWrappedObject();
		Object listWrapper = WrapperFactory.createListWrapper(persistentClassWrapper);
		Value wrappedList = (Value)((Wrapper)listWrapper).getWrappedObject();
		assertTrue(wrappedList instanceof List);
		assertSame(((List)wrappedList).getOwner(), persistentClassTarget);
	}
	
	@Test
	public void testCreateDatabaseReaderWrapper() {
		Properties properties = new Properties();
		properties.put("hibernate.connection.url", "jdbc:h2:mem:test");
		RevengStrategyWrapper strategy = RevengStrategyWrapperFactory.createRevengStrategyWrapper();
		Object databaseReaderWrapper = WrapperFactory.createDatabaseReaderWrapper(
				properties, strategy);
		assertNotNull(databaseReaderWrapper);
		assertTrue(databaseReaderWrapper instanceof DatabaseReaderWrapper);
	}
	
	@Test
	public void testCreateTableWrapper() {
		Object tableWrapper = WrapperFactory.createTableWrapper("foo");
		assertNotNull(tableWrapper);
		assertTrue(tableWrapper instanceof TableWrapper);
		Table table = (Table)((TableWrapper)tableWrapper).getWrappedObject();
		assertEquals("foo", table.getName());
		PrimaryKey pk = table.getPrimaryKey();
		assertSame(table, pk.getTable());
	}

	@Test
	public void testCreateManyToOneWrapper() {
		TableWrapper tableWrapper = TableWrapperFactory.createTableWrapper("foo");
		Table table = (Table)tableWrapper.getWrappedObject();
		Object manyToOneWrapper = WrapperFactory.createManyToOneWrapper(tableWrapper);
		Value wrappedManyToOne = (Value)((Wrapper)manyToOneWrapper).getWrappedObject();
		assertTrue(wrappedManyToOne instanceof ManyToOne);
		assertSame(table, wrappedManyToOne.getTable());
	}

	@Test
	public void testCreateMapWrapper() {
		Object persistentClassWrapper = WrapperFactory.createRootClassWrapper();
		PersistentClass persistentClassTarget = (PersistentClass)((Wrapper)persistentClassWrapper).getWrappedObject();
		Object mapWrapper = WrapperFactory.createMapWrapper(persistentClassWrapper);
		Value wrappedMap = (Value)((Wrapper)mapWrapper).getWrappedObject();
		assertTrue(wrappedMap instanceof Map);
		assertSame(((Map)wrappedMap).getOwner(), persistentClassTarget);
	}
	
	@Test
	public void testCreateOneToManyWrapper() {
		Object persistentClassWrapper = WrapperFactory.createRootClassWrapper();
		PersistentClass persistentClassTarget = (PersistentClass)((Wrapper)persistentClassWrapper).getWrappedObject();
		TableWrapper tableWrapper = (TableWrapper)WrapperFactory.createTableWrapper("foo");
		((RootClass)persistentClassTarget).setTable((Table)tableWrapper.getWrappedObject());
		Object oneToManyWrapper = WrapperFactory.createOneToManyWrapper(persistentClassWrapper);
		Value wrappedOneToMany = (Value)((Wrapper)oneToManyWrapper).getWrappedObject();
		assertTrue(wrappedOneToMany instanceof OneToMany);
		assertSame(((OneToMany)wrappedOneToMany).getTable(), tableWrapper.getWrappedObject());
	}
	
	@Test
	public void testCreateOneToOneWrapper() {
		RootClass rc = new RootClass(DummyMetadataBuildingContext.INSTANCE);
		PersistentClassWrapper persistentClassWrapper = 
				org.hibernate.tool.orm.jbt.internal.factory.PersistentClassWrapperFactory.createPersistentClassWrapper(rc);
		PersistentClass persistentClassTarget = (PersistentClass)persistentClassWrapper.getWrappedObject();
		Table tableTarget = new Table("", "foo");
		((RootClass)persistentClassTarget).setTable(tableTarget);
		persistentClassTarget.setEntityName("bar");
		Object oneToOneWrapper = WrapperFactory.createOneToOneWrapper(persistentClassWrapper);
		Value wrappedOneToOne = (Value)((Wrapper)oneToOneWrapper).getWrappedObject();
		assertTrue(wrappedOneToOne instanceof OneToOne);
		assertEquals(((OneToOne)wrappedOneToOne).getEntityName(), "bar");
		assertSame(((OneToOne)wrappedOneToOne).getTable(), tableTarget);
	}
	
	@Test
	public void testCreatePrimitiveArrayWrapper() {
		Object persistentClassWrapper = WrapperFactory.createRootClassWrapper();
		PersistentClass persistentClassTarget = (PersistentClass)((Wrapper)persistentClassWrapper).getWrappedObject();
		Object primitiveArrayWrapper = WrapperFactory.createPrimitiveArrayWrapper(persistentClassWrapper);
		Value wrappedPrimitiveArray = (Value)((Wrapper)primitiveArrayWrapper).getWrappedObject();
		assertTrue(wrappedPrimitiveArray instanceof PrimitiveArray);
		assertSame(((PrimitiveArray)wrappedPrimitiveArray).getOwner(), persistentClassTarget);
	}

	@Test
	public void testCreateSetWrapper() {
		Object persistentClassWrapper = WrapperFactory.createRootClassWrapper();
		PersistentClass persistentClassTarget = (PersistentClass)((Wrapper)persistentClassWrapper).getWrappedObject();
		Object setWrapper = WrapperFactory.createSetWrapper(persistentClassWrapper);
		Value wrappedSet = (Value)((Wrapper)setWrapper).getWrappedObject();
		assertTrue(wrappedSet instanceof Set);
		assertSame(((Set)wrappedSet).getOwner(), persistentClassTarget);
	}
	
	@Test
	public void testCreateSimpleValueWrapper() {
		Object simpleValueWrapper = WrapperFactory.createSimpleValueWrapper();
		Value wrappedSimpleValue = (Value)((Wrapper)simpleValueWrapper).getWrappedObject();
		assertTrue(wrappedSimpleValue instanceof SimpleValue);
	}
	
	@Test
	public void testCreateComponentWrapper() {
		Object persistentClassWrapper = WrapperFactory.createRootClassWrapper();
		PersistentClass persistentClassTarget = (PersistentClass)((Wrapper)persistentClassWrapper).getWrappedObject();
		Object componentWrapper = WrapperFactory.createComponentWrapper(persistentClassWrapper);
		Value wrappedComponent = (Value)((Wrapper)componentWrapper).getWrappedObject();
		assertTrue(wrappedComponent instanceof Component);
		assertSame(((Component)wrappedComponent).getOwner(), persistentClassTarget);
	}
	
	@Test
	public void testCreateDependantValueWrapper() {
		TableWrapper tableWrapper = TableWrapperFactory.createTableWrapper("foo");
		Table tableTarget = (Table)tableWrapper.getWrappedObject();
		Object valueWrapper = WrapperFactory.createSimpleValueWrapper();
		Object dependantValueWrapper = WrapperFactory.createDependantValueWrapper(tableWrapper, valueWrapper);
		Value wrappedDependantValue = (Value)((Wrapper)dependantValueWrapper).getWrappedObject();
		assertTrue(wrappedDependantValue instanceof DependantValue);
		assertSame(tableTarget, ((DependantValue)wrappedDependantValue).getTable());
		assertSame(
				((DependantValue)wrappedDependantValue).getWrappedValue(), 
				((Wrapper)valueWrapper).getWrappedObject());
	}
	
	@Test
	public void testCreateAnyValueWrapper() {
		TableWrapper tableWrapper = TableWrapperFactory.createTableWrapper("foo");
		Table tableTarget = (Table)tableWrapper.getWrappedObject();
		Object anyValueWrapper = WrapperFactory.createAnyValueWrapper(tableWrapper);
		Value wrappedAnyValue = (Value)((Wrapper)anyValueWrapper).getWrappedObject();
		assertTrue(wrappedAnyValue instanceof Any);
		assertSame(tableTarget, ((Any)wrappedAnyValue).getTable());
	}
	
	@Test
	public void testCreateIdentifierBagValueWrapper() {
		Object persistentClassWrapper = WrapperFactory.createRootClassWrapper();
		PersistentClass persistentClassTarget = (PersistentClass)((Wrapper)persistentClassWrapper).getWrappedObject();
		Object identifierBagValueWrapper = WrapperFactory.createIdentifierBagValueWrapper(persistentClassWrapper);
		Value wrappedIdentifierBagValue = (Value)((Wrapper)identifierBagValueWrapper).getWrappedObject();
		assertTrue(wrappedIdentifierBagValue instanceof IdentifierBag);
		assertSame(((IdentifierBag)wrappedIdentifierBagValue).getOwner(), persistentClassTarget);
	}
	
	@Test
	public void testCreateTableFilterWrapper() {
		Object tableFilterWrapper = WrapperFactory.createTableFilterWrapper();
		assertNotNull(tableFilterWrapper);
		assertTrue(tableFilterWrapper instanceof TableFilterWrapper);
		Object wrappedTableFilter = ((Wrapper)tableFilterWrapper).getWrappedObject();
		assertTrue(wrappedTableFilter instanceof TableFilter);
		
	}
	
	@Test
	public void testCreateTypeFactoryWrapper() {
		Object typeFactoryWrapper = WrapperFactory.createTypeFactoryWrapper();
		assertNotNull(typeFactoryWrapper);
		assertTrue(typeFactoryWrapper instanceof TypeFactoryWrapper);
	}
	
	@Test
	public void testCreateEnvironmentWrapper() {
		assertNotNull(WrapperFactory.createEnvironmentWrapper());
	}
	
	@Test
	public void testCreateSchemaExport() throws Exception {
		ConfigurationWrapper configurationWrapper = 
				ConfigurationWrapperFactory.createNativeConfigurationWrapper();
		Object schemaExport = WrapperFactory.createSchemaExport(configurationWrapper);
		assertNotNull(schemaExport);
		assertTrue(schemaExport instanceof SchemaExportWrapper);
		Field configurationField = schemaExport.getClass().getDeclaredField("configuration");
		configurationField.setAccessible(true);
		assertSame(configurationWrapper.getWrappedObject(), configurationField.get(schemaExport));
	}
	
	@Test
	public void testCreateHbmExporterWrapper() throws Exception {
		ConfigurationWrapper configuration = ConfigurationWrapperFactory.createNativeConfigurationWrapper();
		File file = new File("foo");
		Object hbmExporterWrapper = WrapperFactory.createHbmExporterWrapper(configuration, file);
		HbmExporter wrappedHbmExporter = (HbmExporter)((Wrapper)hbmExporterWrapper).getWrappedObject();
		assertNotNull(hbmExporterWrapper);
		assertSame(file, wrappedHbmExporter.getProperties().get(ExporterConstants.OUTPUT_FILE_NAME));
		ConfigurationMetadataDescriptor descriptor = 
				(ConfigurationMetadataDescriptor)wrappedHbmExporter
					.getProperties()
					.get(ExporterConstants.METADATA_DESCRIPTOR);
		assertNotNull(descriptor);
		Field configurationField = ConfigurationMetadataDescriptor.class.getDeclaredField("configuration");
		configurationField.setAccessible(true);
		assertSame(configuration.getWrappedObject(), configurationField.get(descriptor));
	}
	
	@Test
	public void testCreateExporterWrapper() {
		Object exporterWrapper = WrapperFactory.createExporterWrapper(DdlExporter.class.getName());
		assertNotNull(exporterWrapper);
		assertTrue(exporterWrapper instanceof ExporterWrapper);
	}
	
	@Test
	public void testCreateHqlCodeAssistWrapper() throws Exception {
		ConfigurationWrapper configurationWrapper = ConfigurationWrapperFactory.createNativeConfigurationWrapper();
		configurationWrapper.setProperty("hibernate.connection.url", "jdbc:h2:mem:test");
		Metadata metadata = MetadataHelper.getMetadata((Configuration)configurationWrapper.getWrappedObject());
		Object hqlCodeAssistWrapper = WrapperFactory.createHqlCodeAssistWrapper(configurationWrapper);
		assertTrue(hqlCodeAssistWrapper instanceof HqlCodeAssistWrapper);
		Field metadataField = HQLCodeAssist.class.getDeclaredField("metadata");
		metadataField.setAccessible(true);
		assertSame(metadata, metadataField.get(((Wrapper)hqlCodeAssistWrapper).getWrappedObject()));
		
	}
		
	public static class TestRevengStrategy extends DefaultStrategy {}
	public static class TestDelegatingStrategy extends DelegatingStrategy {
		public TestDelegatingStrategy(RevengStrategy delegate) {
			super(delegate);
		}
	}

}
