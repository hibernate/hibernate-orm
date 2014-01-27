/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PrimaryKeyJoinColumn;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.FilterSource;
import org.hibernate.metamodel.spi.source.JpaCallbackSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

/**
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class EntitySourceImpl implements EntitySource {
	private final EntityClass entityClass;
	private final Set<SubclassEntitySource> subclassEntitySources;
	private final String jpaEntityName;
	private final FilterSource[] filterSources;
	private final TableSpecificationSource primaryTable;
	private final EntityBindingContext bindingContext;
	private final ClassLoaderService classLoaderService;

	public EntitySourceImpl(EntityClass entityClass) {
		this.entityClass = entityClass;
		this.subclassEntitySources = new HashSet<SubclassEntitySource>();

		if ( StringHelper.isNotEmpty( entityClass.getExplicitEntityName() ) ) {
			this.jpaEntityName = entityClass.getExplicitEntityName();
		}
		else {
			this.jpaEntityName = StringHelper.unqualify( entityClass.getName() );
		}
		
		this.bindingContext = entityClass.getLocalBindingContext();
		this.classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );

		addImports();
		this.filterSources = buildFilterSources();
		this.primaryTable = resolvePrimaryTable();
	}

	private TableSpecificationSource resolvePrimaryTable() {
		if ( !entityClass.definesItsOwnTable() ) {
			return null;
		}

		if ( entityClass.hostsAnnotation( HibernateDotNames.SUB_SELECT ) ) {
			return new InLineViewSourceImpl( entityClass );
		}
		else {
			AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.TABLE
			);
			return new TableSourceImpl( tableAnnotation, bindingContext );
		}
	}

	private FilterSource[] buildFilterSources() {
		AnnotationInstance filtersAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(),
				HibernateDotNames.FILTERS ,
				ClassInfo.class
		);
		List<FilterSource> filterSourceList = new ArrayList<FilterSource>();
		if ( filtersAnnotation != null ) {
			AnnotationInstance[] annotationInstances = filtersAnnotation.value().asNestedArray();
			for ( AnnotationInstance filterAnnotation : annotationInstances ) {
				FilterSource filterSource = new FilterSourceImpl( filterAnnotation, bindingContext );
				filterSourceList.add( filterSource );
			}

		}
		AnnotationInstance filterAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(),
				HibernateDotNames.FILTER ,
				ClassInfo.class
		);
		if ( filterAnnotation != null ) {
			FilterSource filterSource = new FilterSourceImpl( filterAnnotation, bindingContext );
			filterSourceList.add( filterSource );
		}
		if ( filterSourceList.isEmpty() ) {
			return null;
		}
		else {
			return filterSourceList.toArray( new FilterSource[filterSourceList.size()] );
		}
	}

	public EntityClass getEntityClass() {
		return entityClass;
	}

	@Override
	public Origin getOrigin() {
		return entityClass.getLocalBindingContext().getOrigin();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return entityClass.getLocalBindingContext();
	}

	@Override
	public String getEntityName() {
		return getClassName();
	}

	@Override
	public String getClassName() {
		return entityClass.getName();
	}

	@Override
	public String getJpaEntityName() {
		return jpaEntityName;
	}

	@Override
	public TableSpecificationSource getPrimaryTable() {
		return primaryTable;
	}

	@Override
	public boolean isAbstract() {
		return entityClass.isAbstract();
	}

	@Override
	public boolean isLazy() {
		return entityClass.isLazy();
	}

	@Override
	public String getProxy() {
		return entityClass.getProxy();
	}

	@Override
	public int getBatchSize() {
		return entityClass.getBatchSize();
	}

	@Override
	public boolean isDynamicInsert() {
		return entityClass.isDynamicInsert();
	}

	@Override
	public boolean isDynamicUpdate() {
		return entityClass.isDynamicUpdate();
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return entityClass.isSelectBeforeUpdate();
	}

	@Override
	public String getCustomTuplizerClassName() {
		return entityClass.getCustomTuplizer();
	}

	@Override
	public String getCustomPersisterClassName() {
		return entityClass.getCustomPersister();
	}

	@Override
	public String getCustomLoaderName() {
		return entityClass.getCustomLoaderQueryName();
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return entityClass.getCustomInsert();
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return entityClass.getCustomUpdate();
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return entityClass.getCustomDelete();
	}

	@Override
	public String[] getSynchronizedTableNames() {
		return entityClass.getSynchronizedTableNames();
	}

	@Override
	public Iterable<MetaAttributeSource> getMetaAttributeSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}

	@Override
	public String getPath() {
		return entityClass.getName();
	}

//	private final ValueHolder<List<AttributeSource>> attributeSource = new ValueHolder<List<AttributeSource>>(
//			new ValueHolder.DeferredInitializer<List<AttributeSource>>() {
//				@Override
//				public List<AttributeSource> initialize() {
//					List<AttributeSource> attributeList = new ArrayList<AttributeSource>();
//					for ( BasicAttribute attribute : getEntityClass().getSimpleAttributes().values() ) {
//						AttributeSource source = new SingularAttributeSourceImpl( attribute );
//						attributeList.add( source );
//					}
//
//					for ( Map.Entry<String, EmbeddableClass> entry : getEntityClass().getEmbeddedClasses().entrySet() ) {
//						final String attributeName = entry.getKey();
//						if ( !getEntityClass().isIdAttribute( attributeName ) ) {
//							final EmbeddableClass component = entry.getValue();
//							attributeList.add(
//									new ComponentAttributeSourceImpl(
//											component,
//											"",
//											getEntityClass().getClassAccessType()
//									)
//							);
//						}
//					}
//					SourceHelper.resolveAssociationAttributes( getEntityClass(), attributeList );
//					return attributeList;
//				}
//			}
//	);

	@Override
	public List<AttributeSource> attributeSources() {
//		return attributeSource.getValue();
		return SourceHelper.resolveAttributes( getEntityClass(), "" ).getValue();
	}

	@Override
	public void add(SubclassEntitySource subclassEntitySource) {
		subclassEntitySources.add( subclassEntitySource );
	}

	@Override
	public FilterSource[] getFilterSources() {
		return filterSources;
	}

	@Override
	public Iterable<SubclassEntitySource> subclassEntitySources() {
		return subclassEntitySources;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return entityClass.getDiscriminatorMatchValue();
	}

	@Override
	public Iterable<ConstraintSource> getConstraints() {
		Set<ConstraintSource> constraintSources = new HashSet<ConstraintSource>();
		
		// primary table
		if ( entityClass.hostsAnnotation( JPADotNames.TABLE ) ) {
			AnnotationInstance table = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.TABLE
			);
			addUniqueConstraints( constraintSources, table, null );
			addIndexConstraints( constraintSources, table, null );
		}

		// secondary table(s)
		if ( entityClass.hostsAnnotation( JPADotNames.SECONDARY_TABLE ) ) {
			AnnotationInstance secondaryTable = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.SECONDARY_TABLE
			);
			String tableName = JandexHelper.getValue( secondaryTable, "name", String.class, classLoaderService );
			addUniqueConstraints( constraintSources, secondaryTable, tableName );
			addIndexConstraints( constraintSources, secondaryTable, tableName );

		}

		if ( entityClass.hostsAnnotation( JPADotNames.SECONDARY_TABLES ) ) {
			AnnotationInstance secondaryTables = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.SECONDARY_TABLES
			);
			if ( secondaryTables != null ) {
				for ( AnnotationInstance secondaryTable : JandexHelper.getValue(
						secondaryTables,
						"value",
						AnnotationInstance[].class,
						classLoaderService
				) ) {
					String tableName = JandexHelper.getValue( secondaryTable, "name", String.class, classLoaderService );
					addUniqueConstraints( constraintSources, secondaryTable, tableName );
					addIndexConstraints( constraintSources, secondaryTable, tableName );
				}
			}
		}

		if ( entityClass.hostsAnnotation( JPADotNames.COLLECTION_TABLE ) ) {
			List<AnnotationInstance> collectionTables = JandexHelper.getAnnotations( 
					entityClass.getClassInfo(), JPADotNames.COLLECTION_TABLE );
			for (AnnotationInstance collectionTable : collectionTables) {
				String tableName = JandexHelper.getValue( collectionTable, "name", String.class, classLoaderService );
				addUniqueConstraints( constraintSources, collectionTable, tableName );
				addIndexConstraints( constraintSources, collectionTable, tableName );
			}

		}

		if ( entityClass.hostsAnnotation( JPADotNames.JOIN_TABLE ) ) {
			List<AnnotationInstance> joinTables = JandexHelper.getAnnotations( 
					entityClass.getClassInfo(), JPADotNames.JOIN_TABLE );
			for (AnnotationInstance joinTable : joinTables) {
				String tableName = JandexHelper.getValue( joinTable, "name", String.class, classLoaderService );
				addUniqueConstraints( constraintSources, joinTable, tableName );
				addIndexConstraints( constraintSources, joinTable, tableName );
			}

		}

		if ( entityClass.hostsAnnotation( JPADotNames.TABLE_GENERATOR ) ) {
			List<AnnotationInstance> tableGenerators = JandexHelper.getAnnotations( 
					entityClass.getClassInfo(), JPADotNames.TABLE_GENERATOR );
			for (AnnotationInstance tableGenerator : tableGenerators) {
				String tableName = JandexHelper.getValue( tableGenerator, "table", String.class, classLoaderService );
				addUniqueConstraints( constraintSources, tableGenerator, tableName );
				addIndexConstraints( constraintSources, tableGenerator, tableName );
			}

		}

		return constraintSources;
	}

	@Override
	public List<JpaCallbackSource> getJpaCallbackClasses() {
		return entityClass.getJpaCallbacks();
	}

	@Override
	public Set<SecondaryTableSource> getSecondaryTables() {
		Set<SecondaryTableSource> secondaryTableSources = new HashSet<SecondaryTableSource>();

		//	process a singular @SecondaryTable annotation
		if ( entityClass.hostsAnnotation( JPADotNames.SECONDARY_TABLE ) ) {
			AnnotationInstance secondaryTable = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.SECONDARY_TABLE
			);
			if ( secondaryTable != null ) {
				secondaryTableSources.add( createSecondaryTableSource( secondaryTable, true ) );
			}
		}

		// process any @SecondaryTables grouping
		if ( entityClass.hostsAnnotation( JPADotNames.SECONDARY_TABLES ) ) {
			AnnotationInstance secondaryTables = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.SECONDARY_TABLES
			);
			if ( secondaryTables != null ) {
				AnnotationInstance[] tableAnnotations = JandexHelper.getValue(
						secondaryTables,
						"value",
						AnnotationInstance[].class,
						classLoaderService
				);
				for ( AnnotationInstance secondaryTable : tableAnnotations ) {
					secondaryTableSources.add( createSecondaryTableSource( secondaryTable, true ) );
				}
			}
		}

		for(AssociationAttribute associationAttribute: entityClass.getAssociationAttributes().values()){
			if( ( associationAttribute.getNature() == MappedAttribute.Nature.MANY_TO_ONE ||
					associationAttribute.getNature() == MappedAttribute.Nature.ONE_TO_ONE ) ) {
				AnnotationInstance joinTableAnnotation = JandexHelper.getSingleAnnotation(
						associationAttribute.annotations(),
						JPADotNames.JOIN_TABLE
				);
				if ( joinTableAnnotation != null ) {
					secondaryTableSources.add( createSecondaryTableSource( joinTableAnnotation, false ) );
				}
			}
		}

		return secondaryTableSources;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntitySourceImpl" );
		sb.append( "{entityClass=" ).append( entityClass.getName() );

		sb.append( ", subclassEntitySources={" );
		for ( SubclassEntitySource subClass : subclassEntitySources ) {
			sb.append( subClass.getClassName() ).append( "," );
		}
		sb.append( "}}" );
		return sb.toString();
	}

	private void addImports() {
		try {
			final MetadataImplementor metadataImplementor = entityClass.getLocalBindingContext()
					.getMetadataImplementor();
			metadataImplementor.addImport( getJpaEntityName(), getEntityName() );
			if ( !getEntityName().equals( getJpaEntityName() ) ) {
				metadataImplementor.addImport( getEntityName(), getEntityName() );
			}
		}
		catch ( MappingException e ) {
			throw new AnnotationException( "Use of the same entity name twice: " + getJpaEntityName(), e );
		}
	}

	private void addUniqueConstraints(Set<ConstraintSource> constraintSources, AnnotationInstance tableAnnotation, String tableName) {
		final AnnotationValue value = tableAnnotation.value( "uniqueConstraints" );
		if ( value == null ) {
			return;
		}

		final AnnotationInstance[] uniqueConstraints = value.asNestedArray();
		for ( final AnnotationInstance unique : uniqueConstraints ) {
			final String name = unique.value( "name" ) == null ? null : unique.value( "name" ).asString();
			final String[] columnNames = unique.value( "columnNames" ).asStringArray();
			final UniqueConstraintSourceImpl uniqueConstraintSource =
					new UniqueConstraintSourceImpl(
							name, tableName, Arrays.asList( columnNames )
					);
			constraintSources.add( uniqueConstraintSource );
		}
	}

	private void addIndexConstraints(Set<ConstraintSource> constraintSources, AnnotationInstance tableAnnotation, String tableName) {
		final AnnotationValue value = tableAnnotation.value( "indexes" );
		if ( value == null ) {
			return;
		}

		final AnnotationInstance[] indexConstraints = value.asNestedArray();
		for ( final AnnotationInstance index : indexConstraints ) {
			final String name = index.value( "name" ) == null ? null : index.value( "name" ).asString();
			final String columnList = index.value( "columnList" ).asString();
			final boolean isUnique = index.value( "unique" ) == null ? false : index.value( "unique" ).asBoolean();
			
			// Taken from JPAIndexHolder.
			// TODO: Move elsewhere?
			final StringTokenizer tokenizer = new StringTokenizer( columnList, "," );
			final List<String> tmp = new ArrayList<String>();
			while ( tokenizer.hasMoreElements() ) {
				tmp.add( tokenizer.nextToken().trim() );
			}
			final List<String> columnNames = new ArrayList<String>();
			final List<String> orderings = new ArrayList<String>();
			for ( String indexColumn : tmp ) {
				indexColumn = indexColumn.toLowerCase();
				if ( indexColumn.endsWith( " desc" ) ) {
					columnNames.add( indexColumn.substring( 0, indexColumn.length() - 5 ) );
					orderings.add( "desc" );
				}
				else if ( indexColumn.endsWith( " asc" ) ) {
					columnNames.add( indexColumn.substring( 0, indexColumn.length() - 4 ) );
					orderings.add( "asc" );
				}
				else {
					columnNames.add( indexColumn );
					orderings.add( null );
				}
			}
			
			ConstraintSource constraintSource = null;
			if ( isUnique ) {
				constraintSource = new UniqueConstraintSourceImpl( name, tableName, columnNames, orderings );
			} else {
				constraintSource = new IndexConstraintSourceImpl( name, tableName, columnNames, orderings );
			}
			constraintSources.add( constraintSource );
		}
	}

	private SecondaryTableSource createSecondaryTableSource(
			AnnotationInstance tableAnnotation,
			boolean isPrimaryKeyJoinColumn) {
		final List<? extends Column> keys = collectSecondaryTableKeys( tableAnnotation, isPrimaryKeyJoinColumn );
		return new SecondaryTableSourceImpl( new TableSourceImpl( tableAnnotation, bindingContext ), keys );
	}

	private List<? extends Column> collectSecondaryTableKeys(
			final AnnotationInstance tableAnnotation,
			final boolean isPrimaryKeyJoinColumn) {
		final AnnotationInstance[] joinColumnAnnotations = JandexHelper.getValue(
				tableAnnotation,
				isPrimaryKeyJoinColumn ? "pkJoinColumns" : "joinColumns",
				AnnotationInstance[].class,
				classLoaderService
		);

		if ( joinColumnAnnotations == null ) {
			return Collections.emptyList();
		}
		final List<Column> keys = new ArrayList<Column>();
		for ( final AnnotationInstance joinColumnAnnotation : joinColumnAnnotations ) {
			final Column joinColumn;
			if ( isPrimaryKeyJoinColumn ) {
				joinColumn =  new PrimaryKeyJoinColumn( joinColumnAnnotation );
			}
			else {
				joinColumn = new Column( joinColumnAnnotation );
			}
			keys.add( joinColumn );
		}
		return keys;
	}
}


