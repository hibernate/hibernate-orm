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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.TruthValue;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.attribute.PersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PrimaryKeyJoinColumn;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.spi.ConstraintSource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.FilterSource;
import org.hibernate.metamodel.source.spi.SecondaryTableSource;
import org.hibernate.metamodel.source.spi.TableSpecificationSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.InFlightMetadataCollector;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.xml.spi.Origin;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * Common base class for adapting Entity classes to the metamodel source structure.
 * <p/>
 * NOTE : defined as abstract because we classify entity mappings more concretely as:<ul>
 *     <li>the root of an entity hierarchy</li>
 *     <li>an entity subclass in an entity hierarchy</li>
 * </ul>
 *
 * @see MappedSuperclassSourceImpl
 *
 * @author Hardy Ferentschik
 * @author Gail Badner
 * @author Steve Ebersole
 */
public abstract class EntitySourceImpl extends IdentifiableTypeSourceAdapter implements EntitySource {
	private final String jpaEntityName;
	private final FilterSource[] filterSources;
	private final TableSpecificationSource primaryTable;
	private final EntityBindingContext bindingContext;
	private final ClassLoaderService classLoaderService;

	/**
	 * This is the form for building the root entity.  FWIW, the `rootEntity`
	 * argument is not really needed here
	 *
	 * @param entityTypeMetadata The root entity
	 * @param hierarchy The hierarchy the entity is the root of
	 * @param rootEntity Whether the entity is a root (it always is here).
	 */
	public EntitySourceImpl(
			EntityTypeMetadata entityTypeMetadata,
			EntityHierarchySourceImpl hierarchy,
			boolean rootEntity) {
		super( entityTypeMetadata, hierarchy, rootEntity );

		this.jpaEntityName = interpretJpaEntityName( entityTypeMetadata );

		this.bindingContext = entityTypeMetadata.getLocalBindingContext();
		this.classLoaderService = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );

		addImports();
		this.filterSources = buildFilterSources();
		this.primaryTable = resolvePrimaryTable();
	}

	private String interpretJpaEntityName(ManagedTypeMetadata managedTypeMetadata) {
		if ( EntityTypeMetadata.class.isInstance( managedTypeMetadata ) ) {
			final EntityTypeMetadata entityTypeMetadata = (EntityTypeMetadata) managedTypeMetadata;
			if ( StringHelper.isNotEmpty( entityTypeMetadata.getExplicitEntityName() ) ) {
				return entityTypeMetadata.getExplicitEntityName();
			}
		}

		return StringHelper.unqualify( managedTypeMetadata.getName() );
	}

	/**
	 * Here is the form for persistent subclasses.
	 *
	 * @param managedTypeMetadata
	 * @param hierarchy
	 * @param superTypeSource
	 */
	protected EntitySourceImpl(
			EntityTypeMetadata managedTypeMetadata,
			EntityHierarchySourceImpl hierarchy,
			IdentifiableTypeSourceAdapter superTypeSource) {
		super( managedTypeMetadata, hierarchy, superTypeSource );

		this.jpaEntityName = interpretJpaEntityName( managedTypeMetadata );

		this.bindingContext = managedTypeMetadata.getLocalBindingContext();
		this.classLoaderService = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );

		addImports();
		this.filterSources = buildFilterSources();
		this.primaryTable = resolvePrimaryTable();
	}

	private void addImports() {
		try {
			final InFlightMetadataCollector metadataImplementor = getEntityClass().getLocalBindingContext()
					.getMetadataCollector();
			metadataImplementor.addImport( getJpaEntityName(), getEntityName() );
			if ( !getEntityName().equals( getJpaEntityName() ) ) {
				metadataImplementor.addImport( getEntityName(), getEntityName() );
			}
		}
		catch ( MappingException e ) {
			throw new AnnotationException( "Use of the same entity name twice: " + getJpaEntityName(), e );
		}
	}

	private FilterSource[] buildFilterSources() {
		AnnotationInstance filtersAnnotation = getEntityClass().getJavaTypeDescriptor().findTypeAnnotation(
				HibernateDotNames.FILTERS
		);
		List<FilterSource> filterSourceList = new ArrayList<FilterSource>();
		if ( filtersAnnotation != null ) {
			AnnotationInstance[] annotationInstances = filtersAnnotation.value().asNestedArray();
			for ( AnnotationInstance filterAnnotation : annotationInstances ) {
				FilterSource filterSource = new FilterSourceImpl( filterAnnotation );
				filterSourceList.add( filterSource );
			}

		}

		AnnotationInstance filterAnnotation = getEntityClass().getJavaTypeDescriptor().findTypeAnnotation(
				HibernateDotNames.FILTER
		);
		if ( filterAnnotation != null ) {
			FilterSource filterSource = new FilterSourceImpl( filterAnnotation );
			filterSourceList.add( filterSource );
		}
		if ( filterSourceList.isEmpty() ) {
			return null;
		}
		else {
			return filterSourceList.toArray( new FilterSource[filterSourceList.size()] );
		}
	}

	protected boolean isRootEntity() {
		return false;
	}

	protected boolean definesItsOwnTable() {
		return !InheritanceType.SINGLE_TABLE.equals( getHierarchy().getHierarchyInheritanceType() )
				|| isRootEntity();
	}

	private TableSpecificationSource resolvePrimaryTable() {
		if ( !definesItsOwnTable() ) {
			return null;
		}

		// see if we have an inline view
		if ( getEntityClass().getJavaTypeDescriptor().findLocalTypeAnnotation( HibernateDotNames.SUB_SELECT ) != null ) {
			return new InLineViewSourceImpl( getEntityClass() );
		}
		else {
			AnnotationInstance tableAnnotation = getEntityClass().getJavaTypeDescriptor().findLocalTypeAnnotation(
					JPADotNames.TABLE
			);
			return buildPrimaryTable( tableAnnotation, bindingContext );
		}
	}

	protected TableSpecificationSource buildPrimaryTable(
			AnnotationInstance tableAnnotation,
			EntityBindingContext bindingContext) {
		return TableSourceImpl.build( tableAnnotation, bindingContext );
	}

	public EntityTypeMetadata getEntityClass() {
		return (EntityTypeMetadata) getManagedTypeMetadata();
	}

	@Override
	public Origin getOrigin() {
		return getEntityClass().getLocalBindingContext().getOrigin();
	}

	@Override
	public EntityBindingContext getLocalBindingContext() {
		return getEntityClass().getLocalBindingContext();
	}

	@Override
	public String getEntityName() {
		return getClassName();
	}

	@Override
	public String getClassName() {
		return getEntityClass().getName();
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
		return getEntityClass().isAbstract();
	}

	@Override
	public boolean isLazy() {
		return getEntityClass().isLazy();
	}

	@Override
	public String getProxy() {
		return getEntityClass().getProxy();
	}

	@Override
	public int getBatchSize() {
		return getEntityClass().getBatchSize();
	}

	@Override
	public boolean isDynamicInsert() {
		return getEntityClass().isDynamicInsert();
	}

	@Override
	public boolean isDynamicUpdate() {
		return getEntityClass().isDynamicUpdate();
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return getEntityClass().isSelectBeforeUpdate();
	}

	@Override
	public String getCustomTuplizerClassName() {
		return getEntityClass().getCustomTuplizerClassName();
	}

	@Override
	public String getCustomPersisterClassName() {
		return getEntityClass().getCustomPersister();
	}

	@Override
	public String getCustomLoaderName() {
		return getEntityClass().getCustomLoaderQueryName();
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return getEntityClass().getCustomInsert();
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return getEntityClass().getCustomUpdate();
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return getEntityClass().getCustomDelete();
	}

	@Override
	public String[] getSynchronizedTableNames() {
		return getEntityClass().getSynchronizedTableNames();
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}

	@Override
	public FilterSource[] getFilterSources() {
		return filterSources;
	}


	@Override
	public String getDiscriminatorMatchValue() {
		return getEntityClass().getDiscriminatorMatchValue();
	}

	@Override
	public Iterable<ConstraintSource> getConstraints() {
		Set<ConstraintSource> constraintSources = new HashSet<ConstraintSource>();

		// primary table
		{
			final AnnotationInstance table = getEntityClass().getJavaTypeDescriptor().findTypeAnnotation(
					JPADotNames.TABLE
			);
			if ( table != null ) {
				addUniqueConstraints( constraintSources, table, null );
				addIndexConstraints( constraintSources, table, null );
			}
		}

		// secondary table
		{
			final AnnotationInstance secondaryTable = getEntityClass().getJavaTypeDescriptor().findTypeAnnotation(
					JPADotNames.SECONDARY_TABLE
			);
			if ( secondaryTable != null ) {
				String tableName = getLocalBindingContext().getJandexAccess()
						.getTypedValueExtractor( String.class )
						.extract( secondaryTable, "name" );
				addUniqueConstraints( constraintSources, secondaryTable, tableName );
				addIndexConstraints( constraintSources, secondaryTable, tableName );

			}
		}


		// secondary tables
		{
			final AnnotationInstance secondaryTables = getEntityClass().getJavaTypeDescriptor().findTypeAnnotation(
					JPADotNames.SECONDARY_TABLES
			);
			if ( secondaryTables != null ) {
				final AnnotationInstance[] secondaryTableArray = getLocalBindingContext().getJandexAccess()
						.getTypedValueExtractor( AnnotationInstance[].class )
						.extract( secondaryTables, "value" );
				for ( AnnotationInstance secondaryTable : secondaryTableArray ) {
					String tableName = getLocalBindingContext().getJandexAccess()
							.getTypedValueExtractor( String.class )
							.extract( secondaryTable, "name" );
					addUniqueConstraints( constraintSources, secondaryTable, tableName );
					addIndexConstraints( constraintSources, secondaryTable, tableName );
				}
			}
		}

		// collection tables
		{
			final Collection<AnnotationInstance> collectionTables = getEntityClass().getJavaTypeDescriptor().findLocalAnnotations(
					JPADotNames.COLLECTION_TABLE
			);

			if ( collectionTables != null ) {
				for ( AnnotationInstance collectionTable : collectionTables ) {
					String tableName = getLocalBindingContext().getJandexAccess()
							.getTypedValueExtractor( String.class )
							.extract( collectionTable, "name" );
					addUniqueConstraints( constraintSources, collectionTable, tableName );
					addIndexConstraints( constraintSources, collectionTable, tableName );
				}
			}
		}

		// join tables
		{
			final Collection<AnnotationInstance> joinTables = getEntityClass().getJavaTypeDescriptor().findLocalAnnotations(
					JPADotNames.JOIN_TABLE
			);
			if ( joinTables != null ) {
				for (AnnotationInstance joinTable : joinTables) {
					String tableName = getLocalBindingContext().getJandexAccess()
							.getTypedValueExtractor( String.class )
							.extract( joinTable, "name" );
					addUniqueConstraints( constraintSources, joinTable, tableName );
					addIndexConstraints( constraintSources, joinTable, tableName );
				}
			}
		}

		// table generators
		{
			final Collection<AnnotationInstance> tableGenerators = getEntityClass().getJavaTypeDescriptor().findLocalAnnotations(
					JPADotNames.TABLE_GENERATOR
			);
			if ( tableGenerators != null ) {
				for (AnnotationInstance tableGenerator : tableGenerators) {
					String tableName = getLocalBindingContext().getJandexAccess()
							.getTypedValueExtractor( String.class )
							.extract( tableGenerator, "table" );
					addUniqueConstraints( constraintSources, tableGenerator, tableName );
					addIndexConstraints( constraintSources, tableGenerator, tableName );
				}
			}
		}

		return constraintSources;
	}

	@Override
	public Set<SecondaryTableSource> getSecondaryTables() {
		Set<SecondaryTableSource> secondaryTableSources = new HashSet<SecondaryTableSource>();

		// todo : should we walk MappedSuperclasses (if any) too?

		//	process a singular @SecondaryTable annotation
		{
			final AnnotationInstance secondaryTable = getEntityClass().getJavaTypeDescriptor().findLocalTypeAnnotation(
					JPADotNames.SECONDARY_TABLE
			);
			if ( secondaryTable != null ) {
				secondaryTableSources.add( createSecondaryTableSource( secondaryTable, true ) );
			}
		}

		// process any @SecondaryTables grouping
		{
			final AnnotationInstance secondaryTables = getEntityClass().getJavaTypeDescriptor().findLocalTypeAnnotation(
					JPADotNames.SECONDARY_TABLES
			);
			if ( secondaryTables != null ) {
				AnnotationInstance[] tableAnnotations = getLocalBindingContext().getJandexAccess()
						.getTypedValueExtractor( AnnotationInstance[].class )
						.extract( secondaryTables, "value" );
				for ( AnnotationInstance secondaryTable : tableAnnotations ) {
					secondaryTableSources.add( createSecondaryTableSource( secondaryTable, true ) );
				}
			}
		}

		for ( PersistentAttribute attribute : getEntityClass().getPersistentAttributeMap().values() ) {
			if ( attribute.getNature() == PersistentAttribute.Nature.MANY_TO_ONE
					|| attribute.getNature() == PersistentAttribute.Nature.ONE_TO_ONE ) {
				AnnotationInstance joinTableAnnotation = attribute.getBackingMember()
						.getAnnotations()
						.get( JPADotNames.JOIN_TABLE );
				if ( joinTableAnnotation != null ) {
					secondaryTableSources.add( createSecondaryTableSource( joinTableAnnotation, false ) );
				}
			}
		}

		return secondaryTableSources;
	}

	@Override
	public String toString() {
		return "EntitySourceImpl{entityClass=" + getEntityClass().getName() + "}";
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
			
			ConstraintSource constraintSource = new IndexConstraintSourceImpl(
					name, tableName, columnNames, orderings, isUnique );
			constraintSources.add( constraintSource );
		}
	}

	private SecondaryTableSource createSecondaryTableSource(
			AnnotationInstance tableAnnotation,
			boolean isPrimaryKeyJoinColumn) {
		final List<? extends Column> keys = collectSecondaryTableKeys( tableAnnotation, isPrimaryKeyJoinColumn );
		return new SecondaryTableSourceImpl( TableSourceImpl.build( tableAnnotation, bindingContext ), keys );
	}

	private List<? extends Column> collectSecondaryTableKeys(
			final AnnotationInstance tableAnnotation,
			final boolean isPrimaryKeyJoinColumn) {
		final AnnotationInstance[] joinColumnAnnotations = getLocalBindingContext()
				.getJandexAccess()
				.getTypedValueExtractor( AnnotationInstance[].class )
				.extract( tableAnnotation, isPrimaryKeyJoinColumn ? "pkJoinColumns" : "joinColumns" );

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

	@Override
	public String getTypeName() {
		return getEntityClass().getName();
	}

	@Override
	public TruthValue quoteIdentifiersLocalToEntity() {
		// not exposed atm
		return TruthValue.UNKNOWN;
	}
}


