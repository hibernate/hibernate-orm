/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.hibernate.EntityMode;
import org.hibernate.TruthValue;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.internal.annotations.AnnotationMetadataSourceProcessor;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.IdentifierGeneratorSource;
import org.hibernate.metamodel.source.spi.IdentifierSource;
import org.hibernate.metamodel.source.spi.SimpleIdentifierSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.TableSource;
import org.hibernate.metamodel.source.spi.TableSpecificationSource;
import org.hibernate.metamodel.spi.BindingContext;
import org.hibernate.metamodel.spi.SingularAttributeNature;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.jboss.jandex.IndexView;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class AssertSourcesTest extends BaseUnitTestCase {
	final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build() ;

	// TODO
//	@Test
//	public void testUserEntitySourcesHbm() {
//		BindingContext rootContext = RootBindingContextBuilder.buildBindingContext( serviceRegistry );
//
//		MetadataSources hbm = new MetadataSources( serviceRegistry );
//		hbm.addResource( getClass().getPackage().getName().replace( '.', '/' ) + "/User.hbm.xml" );
//		MetadataSourceProcessor hbmProcessor = new HbmMetadataSourceProcessorImpl(
//				rootContext,
//				hbm
//		);
//		testUserEntitySources( hbmProcessor, rootContext );
//	}

	@Test
	public void testUserEntitySourcesAnnotations() {
		MetadataSources ann = new MetadataSources( serviceRegistry );
		ann.addAnnotatedClass( User.class );
		ann.addAnnotatedClass( User.Name.class );

		final IndexView indexView = ann.buildJandexView();
		BindingContext rootContext = RootBindingContextBuilder.buildBindingContext( serviceRegistry, indexView );

		AnnotationMetadataSourceProcessor annProcessor = new AnnotationMetadataSourceProcessor(
				rootContext,
				indexView
		);
		testUserEntitySources( annProcessor, rootContext );
	}

	private void testUserEntitySources(AnnotationMetadataSourceProcessor processor, BindingContext rootContext) {
		for ( IdentifierGeneratorSource identifierGeneratorSource : processor.extractGlobalIdentifierGeneratorSources() ) {
			rootContext.getMetadataCollector().addIdGenerator(
					new IdentifierGeneratorDefinition(
							identifierGeneratorSource.getGeneratorName(),
							identifierGeneratorSource.getGeneratorImplementationName(),
							identifierGeneratorSource.getParameters()
					)
			);
		}

		Iterator<EntityHierarchySource> hierarchies = processor.extractEntityHierarchies().iterator();
		assertTrue( hierarchies.hasNext() );
		EntityHierarchySource hierarchy = hierarchies.next();
		assertFalse( hierarchies.hasNext() );
		assertTrue( hierarchy.getHierarchyInheritanceType() == InheritanceType.NO_INHERITANCE );

		EntitySource entitySource = hierarchy.getRoot();
		assertNotNull( entitySource );
		assertEquals( 0, entitySource.getSubTypes().size() );

		assertEquals( User.class.getName(), entitySource.getClassName() );
		assertEquals( User.class.getName(), entitySource.getEntityName() );
		assertEquals( StringHelper.unqualify( User.class.getName() ), entitySource.getJpaEntityName() );

		assertEquals( EntityMode.POJO, hierarchy.getEntityMode() );
		assertEquals( TruthValue.FALSE, hierarchy.getCaching().getRequested() );
		assertNull( hierarchy.getDiscriminatorSource() );
		assertNull( entitySource.getDiscriminatorMatchValue() );

		assertTrue( entitySource.getJpaCallbackClasses() == null || entitySource.getJpaCallbackClasses().isEmpty() );

		TableSpecificationSource primaryTableSpecificationSource = entitySource.getPrimaryTable();
		assertTrue( TableSource.class.isInstance( primaryTableSpecificationSource  ) );
		TableSource primaryTable = (TableSource) primaryTableSpecificationSource;
		// todo : should sources be responsible for figuring out logical names?
		//		these are the things that need to match in terms of lookup keys
		assertNull( primaryTable.getExplicitCatalogName() );
		assertNull( primaryTable.getExplicitSchemaName() );
		assertNull( primaryTable.getExplicitTableName() );

		assertFalse( entitySource.getSecondaryTables().iterator().hasNext() );
		assertTrue(
				ArrayHelper.isEmpty( entitySource.getSynchronizedTableNames() )
		);

		IdentifierSource identifierSource = hierarchy.getIdentifierSource();
		assertNotNull( identifierSource );
		assertEquals( EntityIdentifierNature.SIMPLE, identifierSource.getNature() );
		SimpleIdentifierSource simpleIdentifierSource = (SimpleIdentifierSource) identifierSource;
		SingularAttributeSource identifierAttributeSource = simpleIdentifierSource.getIdentifierAttributeSource();
		assertEquals( "id", identifierAttributeSource.getName() );
		assertTrue( identifierAttributeSource.isSingular() );
		assertFalse( identifierAttributeSource.isVirtualAttribute() );
		assertFalse( identifierAttributeSource.isLazy() );
		assertEquals( SingularAttributeNature.BASIC, identifierAttributeSource.getSingularAttributeNature() );
		assertEquals( PropertyGeneration.INSERT, identifierAttributeSource.getGeneration() );
//		assertNull( identifierAttributeSource.getPropertyAccessorName() );

		// todo : here is an interesting question in terms of who is responsible for semantic interpretation
		//		really an attribute for identifier should never be included in updates
		//		and it should only be included in inserts only in certain cases
		//		and should never be nullable
		//	^^ all because this is the PK.  That is the semantic interpretation of those values based on that context.
		//
		// So do sources simply return explicit user values?  Or do they consider such semantic analysis?
		assertTrue( identifierAttributeSource.areValuesIncludedInInsertByDefault() );
		assertFalse( identifierAttributeSource.areValuesIncludedInUpdateByDefault() );
		assertFalse( identifierAttributeSource.areValuesNullableByDefault() );

		// todo : return collections?  or iterables?
		// todo : empty collections?  or null?
		assertEquals( 0, identifierAttributeSource.relationalValueSources().size() );
//		RelationalValueSource relationalValueSource = identifierAttributeSource.relationalValueSources().get( 0 );
//		assertNull( relationalValueSource.getContainingTableName() );
//		assertEquals( RelationalValueSource.Nature.COLUMN, relationalValueSource.getNature() );
//		ColumnSource columnSource = (ColumnSource) relationalValueSource;
//		assertNull( columnSource.getName() );
//		assertNull( columnSource.getReadFragment() );
//		assertNull( columnSource.getWriteFragment() );
//		assertNull( columnSource.getDefaultValue() );
//		assertNull( columnSource.getSqlType() );
//		assertNull( columnSource.getCheckCondition() );
//		assertNull( columnSource.getComment() );
//		assertNull( columnSource.getJdbcDataType() );
//		assertNull( columnSource.getSize() );
//		// todo : technically, pk has to be unique, but this another semantic case
//		assertFalse( columnSource.isUnique() );
//		// todo : see comments above
//		assertFalse( columnSource.isIncludedInInsert() );
//		assertFalse( columnSource.isIncludedInUpdate() );
//		assertFalse( columnSource.isNullable() );

		assertEquals( 3, entitySource.attributeSources().size() );


	}

	@Test
	public void testOrderEntitySources() {
		MetadataSources ann = new MetadataSources( serviceRegistry );
		ann.addAnnotatedClass( Order.class );
		ann.addAnnotatedClass( Order.class );
		ann.addAnnotatedClass( Order.OrderPk.class );
		IndexView indexView = ann.buildJandexView();
		AnnotationMetadataSourceProcessor annProcessor = new AnnotationMetadataSourceProcessor(
				RootBindingContextBuilder.buildBindingContext( serviceRegistry, indexView ),
				indexView
		);
		testOrderEntitySources( annProcessor );
	}

	private void testOrderEntitySources(AnnotationMetadataSourceProcessor processor) {
		Iterator<EntityHierarchySource> hierarchies = processor.extractEntityHierarchies().iterator();
		assertTrue( hierarchies.hasNext() );
		EntityHierarchySource hierarchy = hierarchies.next();
		assertFalse( hierarchies.hasNext() );
		assertTrue( hierarchy.getHierarchyInheritanceType() == InheritanceType.NO_INHERITANCE );

		EntitySource entitySource = hierarchy.getRoot();
		assertEquals( 0, entitySource.getSubTypes().size() );

		assertEquals( Order.class.getName(), entitySource.getClassName() );
		assertEquals( Order.class.getName(), entitySource.getEntityName() );
		assertEquals( StringHelper.unqualify( Order.class.getName() ), entitySource.getJpaEntityName() );
	}

	@Test
	public void testOrderNonAggregatedEntitySources() {
		MetadataSources ann = new MetadataSources( serviceRegistry );
		ann.addAnnotatedClass( Order.class );
		ann.addAnnotatedClass( Order.class );
		ann.addAnnotatedClass( Order.OrderPk.class );
		final IndexView indexView = ann.buildJandexView();
		AnnotationMetadataSourceProcessor annProcessor = new AnnotationMetadataSourceProcessor(
				RootBindingContextBuilder.buildBindingContext( serviceRegistry, indexView ),
				indexView
		);
		testOrderNonAggregatedEntitySources( annProcessor );
	}

	private void testOrderNonAggregatedEntitySources(AnnotationMetadataSourceProcessor processor) {
		Iterator<EntityHierarchySource> hierarchies = processor.extractEntityHierarchies().iterator();
		assertTrue( hierarchies.hasNext() );
		EntityHierarchySource hierarchy = hierarchies.next();
		assertFalse( hierarchies.hasNext() );
		assertTrue( hierarchy.getHierarchyInheritanceType() == InheritanceType.NO_INHERITANCE );

		EntitySource entitySource = hierarchy.getRoot();
		assertEquals( 0, entitySource.getSubTypes().size() );

		assertEquals( Order.class.getName(), entitySource.getClassName() );
		assertEquals( Order.class.getName(), entitySource.getEntityName() );
		assertEquals( StringHelper.unqualify( Order.class.getName() ), entitySource.getJpaEntityName() );
	}
}
