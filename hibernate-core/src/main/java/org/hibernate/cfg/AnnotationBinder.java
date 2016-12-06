/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.SequenceGenerator;
import javax.persistence.SharedCacheMode;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.GenericGenerators;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.Source;
import org.hibernate.annotations.Tuplizer;
import org.hibernate.annotations.Tuplizers;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.common.reflection.ClassLoadingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.spi.InFlightMetadataCollector.EntityTableXref;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.CollectionBinder;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.cfg.annotations.HCANNHelper;
import org.hibernate.cfg.annotations.MapKeyColumnDelegator;
import org.hibernate.cfg.annotations.MapKeyJoinColumnDelegator;
import org.hibernate.cfg.annotations.Nullability;
import org.hibernate.cfg.annotations.PropertyBinder;
import org.hibernate.cfg.annotations.QueryBinder;
import org.hibernate.cfg.annotations.SimpleValueBinder;
import org.hibernate.cfg.annotations.TableBinder;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.PropertyPath;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdGenerator;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.UnionSubclass;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * JSR 175 annotation binder which reads the annotations from classes, applies the
 * principles of the EJB3 spec and produces the Hibernate configuration-time metamodel
 * (the classes in the {@code org.hibernate.mapping} package)
 * <p/>
 * Some design description
 * I tried to remove any link to annotation except from the 2 first level of
 * method call.
 * It'll enable to:
 *   - facilitate annotation overriding
 *   - mutualize one day xml and annotation binder (probably a dream though)
 *   - split this huge class in smaller mapping oriented classes
 *
 * bindSomething usually create the mapping container and is accessed by one of the 2 first level method
 * makeSomething usually create the mapping container and is accessed by bindSomething[else]
 * fillSomething take the container into parameter and fill it.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public final class AnnotationBinder {
	private static final CoreMessageLogger LOG = messageLogger( AnnotationBinder.class );

	private AnnotationBinder() {
	}

	public static void bindDefaults(MetadataBuildingContext context) {
		Map defaults = context.getBuildingOptions().getReflectionManager().getDefaults();

		// id generators ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		{
			List<SequenceGenerator> anns = ( List<SequenceGenerator> ) defaults.get( SequenceGenerator.class );
			if ( anns != null ) {
				for ( SequenceGenerator ann : anns ) {
					IdentifierGeneratorDefinition idGen = buildIdGenerator( ann, context );
					if ( idGen != null ) {
						context.getMetadataCollector().addDefaultIdentifierGenerator( idGen );
					}
				}
			}
		}
		{
			List<TableGenerator> anns = ( List<TableGenerator> ) defaults.get( TableGenerator.class );
			if ( anns != null ) {
				for ( TableGenerator ann : anns ) {
					IdentifierGeneratorDefinition idGen = buildIdGenerator( ann, context );
					if ( idGen != null ) {
						context.getMetadataCollector().addDefaultIdentifierGenerator( idGen );
					}
				}
			}
		}

		// queries ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		{
			List<NamedQuery> anns = ( List<NamedQuery> ) defaults.get( NamedQuery.class );
			if ( anns != null ) {
				for ( NamedQuery ann : anns ) {
					QueryBinder.bindQuery( ann, context, true );
				}
			}
		}
		{
			List<NamedNativeQuery> anns = ( List<NamedNativeQuery> ) defaults.get( NamedNativeQuery.class );
			if ( anns != null ) {
				for ( NamedNativeQuery ann : anns ) {
					QueryBinder.bindNativeQuery( ann, context, true );
				}
			}
		}

		// result-set-mappings ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		{
			List<SqlResultSetMapping> anns = ( List<SqlResultSetMapping> ) defaults.get( SqlResultSetMapping.class );
			if ( anns != null ) {
				for ( SqlResultSetMapping ann : anns ) {
					QueryBinder.bindSqlResultSetMapping( ann, context, true );
				}
			}
		}

		// stored procs ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		{
			final List<NamedStoredProcedureQuery> annotations =
					(List<NamedStoredProcedureQuery>) defaults.get( NamedStoredProcedureQuery.class );
			if ( annotations != null ) {
				for ( NamedStoredProcedureQuery annotation : annotations ) {
					bindNamedStoredProcedureQuery( annotation, context, true );
				}
			}
		}
		{
			final List<NamedStoredProcedureQueries> annotations =
					(List<NamedStoredProcedureQueries>) defaults.get( NamedStoredProcedureQueries.class );
			if ( annotations != null ) {
				for ( NamedStoredProcedureQueries annotation : annotations ) {
					bindNamedStoredProcedureQueries( annotation, context, true );
				}
			}
		}
	}

	public static void bindPackage(String packageName, MetadataBuildingContext context) {
		XPackage pckg;
		try {
			pckg = context.getBuildingOptions().getReflectionManager().packageForName( packageName );
		}
		catch (ClassLoadingException e) {
			LOG.packageNotFound( packageName );
			return;
		}
		catch ( ClassNotFoundException cnf ) {
			LOG.packageNotFound( packageName );
			return;
		}

		if ( pckg.isAnnotationPresent( SequenceGenerator.class ) ) {
			SequenceGenerator ann = pckg.getAnnotation( SequenceGenerator.class );
			IdentifierGeneratorDefinition idGen = buildIdGenerator( ann, context );
			context.getMetadataCollector().addIdentifierGenerator( idGen );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add sequence generator with name: {0}", idGen.getName() );
			}
		}

		if ( pckg.isAnnotationPresent( TableGenerator.class ) ) {
			TableGenerator ann = pckg.getAnnotation( TableGenerator.class );
			IdentifierGeneratorDefinition idGen = buildIdGenerator( ann, context );
			context.getMetadataCollector().addIdentifierGenerator( idGen );
		}

		bindGenericGenerators( pckg, context );
		bindQueries( pckg, context );
		bindFilterDefs( pckg, context );
		bindTypeDefs( pckg, context );
		bindFetchProfiles( pckg, context );
		BinderHelper.bindAnyMetaDefs( pckg, context );

	}

	private static void bindGenericGenerators(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		GenericGenerator defAnn = annotatedElement.getAnnotation( GenericGenerator.class );
		GenericGenerators defsAnn = annotatedElement.getAnnotation( GenericGenerators.class );
		if ( defAnn != null ) {
			bindGenericGenerator( defAnn, context );
		}
		if ( defsAnn != null ) {
			for ( GenericGenerator def : defsAnn.value() ) {
				bindGenericGenerator( def, context );
			}
		}
	}

	private static void bindGenericGenerator(GenericGenerator def, MetadataBuildingContext context) {
		context.getMetadataCollector().addIdentifierGenerator( buildIdGenerator( def, context ) );
	}

	private static void bindQueries(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		{
			SqlResultSetMapping ann = annotatedElement.getAnnotation( SqlResultSetMapping.class );
			QueryBinder.bindSqlResultSetMapping( ann, context, false );
		}
		{
			SqlResultSetMappings ann = annotatedElement.getAnnotation( SqlResultSetMappings.class );
			if ( ann != null ) {
				for ( SqlResultSetMapping current : ann.value() ) {
					QueryBinder.bindSqlResultSetMapping( current, context, false );
				}
			}
		}
		{
			NamedQuery ann = annotatedElement.getAnnotation( NamedQuery.class );
			QueryBinder.bindQuery( ann, context, false );
		}
		{
			org.hibernate.annotations.NamedQuery ann = annotatedElement.getAnnotation(
					org.hibernate.annotations.NamedQuery.class
			);
			QueryBinder.bindQuery( ann, context );
		}
		{
			NamedQueries ann = annotatedElement.getAnnotation( NamedQueries.class );
			QueryBinder.bindQueries( ann, context, false );
		}
		{
			org.hibernate.annotations.NamedQueries ann = annotatedElement.getAnnotation(
					org.hibernate.annotations.NamedQueries.class
			);
			QueryBinder.bindQueries( ann, context );
		}
		{
			NamedNativeQuery ann = annotatedElement.getAnnotation( NamedNativeQuery.class );
			QueryBinder.bindNativeQuery( ann, context, false );
		}
		{
			org.hibernate.annotations.NamedNativeQuery ann = annotatedElement.getAnnotation(
					org.hibernate.annotations.NamedNativeQuery.class
			);
			QueryBinder.bindNativeQuery( ann, context );
		}
		{
			NamedNativeQueries ann = annotatedElement.getAnnotation( NamedNativeQueries.class );
			QueryBinder.bindNativeQueries( ann, context, false );
		}
		{
			org.hibernate.annotations.NamedNativeQueries ann = annotatedElement.getAnnotation(
					org.hibernate.annotations.NamedNativeQueries.class
			);
			QueryBinder.bindNativeQueries( ann, context );
		}

		// NamedStoredProcedureQuery handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		bindNamedStoredProcedureQuery(
				annotatedElement.getAnnotation( NamedStoredProcedureQuery.class ),
				context,
				false
		);

		// NamedStoredProcedureQueries handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		bindNamedStoredProcedureQueries(
				annotatedElement.getAnnotation( NamedStoredProcedureQueries.class ),
				context,
				false
		);
	}

	private static void bindNamedStoredProcedureQueries(NamedStoredProcedureQueries annotation, MetadataBuildingContext context, boolean isDefault) {
		if ( annotation != null ) {
			for ( NamedStoredProcedureQuery queryAnnotation : annotation.value() ) {
				bindNamedStoredProcedureQuery( queryAnnotation, context, isDefault );
			}
		}
	}

	private static void bindNamedStoredProcedureQuery(NamedStoredProcedureQuery annotation, MetadataBuildingContext context, boolean isDefault) {
		if ( annotation != null ) {
			QueryBinder.bindNamedStoredProcedureQuery( annotation, context, isDefault );
		}
	}

	private static IdentifierGeneratorDefinition buildIdGenerator(java.lang.annotation.Annotation ann, MetadataBuildingContext context) {
		if ( ann == null ) {
			return null;
		}

		IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();

		if ( context.getMappingDefaults().getImplicitSchemaName() != null ) {
			definitionBuilder.addParam(
					PersistentIdentifierGenerator.SCHEMA,
					context.getMappingDefaults().getImplicitSchemaName()
			);
		}

		if ( context.getMappingDefaults().getImplicitCatalogName() != null ) {
			definitionBuilder.addParam(
					PersistentIdentifierGenerator.CATALOG,
					context.getMappingDefaults().getImplicitCatalogName()
			);
		}

		if ( ann instanceof TableGenerator ) {
			context.getBuildingOptions().getIdGenerationTypeInterpreter().interpretTableGenerator(
					(TableGenerator) ann,
					definitionBuilder
			);
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add table generator with name: {0}", definitionBuilder.getName() );
			}
		}
		else if ( ann instanceof SequenceGenerator ) {
			context.getBuildingOptions().getIdGenerationTypeInterpreter().interpretSequenceGenerator(
					(SequenceGenerator) ann,
					definitionBuilder
			);
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add sequence generator with name: {0}", definitionBuilder.getName() );
			}
		}
		else if ( ann instanceof GenericGenerator ) {
			GenericGenerator genGen = ( GenericGenerator ) ann;
			definitionBuilder.setName( genGen.name() );
			definitionBuilder.setStrategy( genGen.strategy() );
			Parameter[] params = genGen.parameters();
			for ( Parameter parameter : params ) {
				definitionBuilder.addParam( parameter.name(), parameter.value() );
			}
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add generic generator with name: {0}", definitionBuilder.getName() );
			}
		}
		else {
			throw new AssertionFailure( "Unknown Generator annotation: " + ann );
		}

		return definitionBuilder.build();
	}

	/**
	 * Bind a class having JSR175 annotations. Subclasses <b>have to</b> be bound afterQuery its parent class.
	 *
	 * @param clazzToProcess entity to bind as {@code XClass} instance
	 * @param inheritanceStatePerClass Meta data about the inheritance relationships for all mapped classes
	 *
	 * @throws MappingException in case there is an configuration error
	 */
	public static void bindClass(
			XClass clazzToProcess,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext context) throws MappingException {
		//@Entity and @MappedSuperclass on the same class leads to a NPE down the road
		if ( clazzToProcess.isAnnotationPresent( Entity.class )
				&&  clazzToProcess.isAnnotationPresent( MappedSuperclass.class ) ) {
			throw new AnnotationException( "An entity cannot be annotated with both @Entity and @MappedSuperclass: "
					+ clazzToProcess.getName() );
		}

		//TODO: be more strict with secondarytable allowance (not for ids, not for secondary table join columns etc)
		InheritanceState inheritanceState = inheritanceStatePerClass.get( clazzToProcess );
		AnnotatedClassType classType = context.getMetadataCollector().getClassType( clazzToProcess );

		//Queries declared in MappedSuperclass should be usable in Subclasses
		if ( AnnotatedClassType.EMBEDDABLE_SUPERCLASS.equals( classType ) ) {
			bindQueries( clazzToProcess, context );
			bindTypeDefs( clazzToProcess, context );
			bindFilterDefs( clazzToProcess, context );
		}

		if ( !isEntityClassType( clazzToProcess, classType ) ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding entity from annotated class: %s", clazzToProcess.getName() );
		}

		PersistentClass superEntity = getSuperEntity(
				clazzToProcess,
				inheritanceStatePerClass,
				context,
				inheritanceState
		);

		PersistentClass persistentClass = makePersistentClass( inheritanceState, superEntity, context );
		Entity entityAnn = clazzToProcess.getAnnotation( Entity.class );
		org.hibernate.annotations.Entity hibEntityAnn = clazzToProcess.getAnnotation(
				org.hibernate.annotations.Entity.class
		);
		EntityBinder entityBinder = new EntityBinder(
				entityAnn,
				hibEntityAnn,
				clazzToProcess,
				persistentClass,
				context
		);
		entityBinder.setInheritanceState( inheritanceState );

		bindQueries( clazzToProcess, context );
		bindFilterDefs( clazzToProcess, context );
		bindTypeDefs( clazzToProcess, context );
		bindFetchProfiles( clazzToProcess, context );
		BinderHelper.bindAnyMetaDefs( clazzToProcess, context );

		String schema = "";
		String table = ""; //might be no @Table annotation on the annotated class
		String catalog = "";
		List<UniqueConstraintHolder> uniqueConstraints = new ArrayList<UniqueConstraintHolder>();
		javax.persistence.Table tabAnn = null;
		if ( clazzToProcess.isAnnotationPresent( javax.persistence.Table.class ) ) {
			tabAnn = clazzToProcess.getAnnotation( javax.persistence.Table.class );
			table = tabAnn.name();
			schema = tabAnn.schema();
			catalog = tabAnn.catalog();
			uniqueConstraints = TableBinder.buildUniqueConstraintHolders( tabAnn.uniqueConstraints() );
		}

		Ejb3JoinColumn[] inheritanceJoinedColumns = makeInheritanceJoinColumns(
				clazzToProcess,
				context,
				inheritanceState,
				superEntity
		);

		final Ejb3DiscriminatorColumn discriminatorColumn;
		if ( InheritanceType.SINGLE_TABLE.equals( inheritanceState.getType() ) ) {
			discriminatorColumn = processSingleTableDiscriminatorProperties(
					clazzToProcess,
					context,
					inheritanceState,
					entityBinder
			);
		}
		else if ( InheritanceType.JOINED.equals( inheritanceState.getType() ) ) {
			discriminatorColumn = processJoinedDiscriminatorProperties(
					clazzToProcess,
					context,
					inheritanceState,
					entityBinder
			);
		}
		else {
			discriminatorColumn = null;
		}

		entityBinder.setProxy( clazzToProcess.getAnnotation( Proxy.class ) );
		entityBinder.setBatchSize( clazzToProcess.getAnnotation( BatchSize.class ) );
		entityBinder.setWhere( clazzToProcess.getAnnotation( Where.class ) );
		entityBinder.setCache( determineCacheSettings( clazzToProcess, context ) );
		entityBinder.setNaturalIdCache( clazzToProcess, clazzToProcess.getAnnotation( NaturalIdCache.class ) );

		bindFilters( clazzToProcess, entityBinder, context );

		entityBinder.bindEntity();

		if ( inheritanceState.hasTable() ) {
			Check checkAnn = clazzToProcess.getAnnotation( Check.class );
			String constraints = checkAnn == null
					? null
					: checkAnn.constraints();

			EntityTableXref denormalizedTableXref = inheritanceState.hasDenormalizedTable()
					? context.getMetadataCollector().getEntityTableXref( superEntity.getEntityName() )
					: null;

			entityBinder.bindTable(
					schema,
					catalog,
					table,
					uniqueConstraints,
					constraints,
					denormalizedTableXref
			);
		}
		else {
			if ( clazzToProcess.isAnnotationPresent( Table.class ) ) {
				LOG.invalidTableAnnotation( clazzToProcess.getName() );
			}

			if ( inheritanceState.getType() == InheritanceType.SINGLE_TABLE ) {
				// we at least need to properly set up the EntityTableXref
				entityBinder.bindTableForDiscriminatedSubclass(
						context.getMetadataCollector().getEntityTableXref( superEntity.getEntityName() )
				);
			}
		}


		PropertyHolder propertyHolder = PropertyHolderBuilder.buildPropertyHolder(
				clazzToProcess,
				persistentClass,
				entityBinder,
				context,
				inheritanceStatePerClass
		);

		javax.persistence.SecondaryTable secTabAnn = clazzToProcess.getAnnotation(
				javax.persistence.SecondaryTable.class
		);
		javax.persistence.SecondaryTables secTabsAnn = clazzToProcess.getAnnotation(
				javax.persistence.SecondaryTables.class
		);
		entityBinder.firstLevelSecondaryTablesBinding( secTabAnn, secTabsAnn );

		OnDelete onDeleteAnn = clazzToProcess.getAnnotation( OnDelete.class );
		boolean onDeleteAppropriate = false;

		// todo : sucks that this is separate from RootClass distinction
		final boolean isInheritanceRoot = !inheritanceState.hasParents();
		final boolean hasSubclasses = inheritanceState.hasSiblings();

		if ( InheritanceType.JOINED.equals( inheritanceState.getType() ) ) {
			if ( inheritanceState.hasParents() ) {
				onDeleteAppropriate = true;
				final JoinedSubclass jsc = ( JoinedSubclass ) persistentClass;
				SimpleValue key = new DependantValue( context.getMetadataCollector(), jsc.getTable(), jsc.getIdentifier() );
				jsc.setKey( key );
				ForeignKey fk = clazzToProcess.getAnnotation( ForeignKey.class );
				if ( fk != null && !BinderHelper.isEmptyAnnotationValue( fk.name() ) ) {
					key.setForeignKeyName( fk.name() );
				}
				else {
					final PrimaryKeyJoinColumn pkJoinColumn = clazzToProcess.getAnnotation( PrimaryKeyJoinColumn.class );
					if ( pkJoinColumn != null && pkJoinColumn.foreignKey() != null
							&& !StringHelper.isEmpty( pkJoinColumn.foreignKey().name() ) ) {
						key.setForeignKeyName( pkJoinColumn.foreignKey().name() );
					}
				}
				if ( onDeleteAnn != null ) {
					key.setCascadeDeleteEnabled( OnDeleteAction.CASCADE.equals( onDeleteAnn.action() ) );
				}
				else {
					key.setCascadeDeleteEnabled( false );
				}
				//we are never in a second pass at that stage, so queue it
				context.getMetadataCollector().addSecondPass( new JoinedSubclassFkSecondPass( jsc, inheritanceJoinedColumns, key, context ) );
				context.getMetadataCollector().addSecondPass( new CreateKeySecondPass( jsc ) );
			}

			if ( isInheritanceRoot ) {
				// the class we are processing is the root of the hierarchy, see if we had a discriminator column
				// (it is perfectly valid for joined subclasses to not have discriminators).
				if ( discriminatorColumn != null ) {
					// we have a discriminator column
					if ( hasSubclasses || !discriminatorColumn.isImplicit() ) {
						bindDiscriminatorColumnToRootPersistentClass(
								(RootClass) persistentClass,
								discriminatorColumn,
								entityBinder.getSecondaryTables(),
								propertyHolder,
								context
						);
						//bind it again since the type might have changed
						entityBinder.bindDiscriminatorValue();
					}
				}
			}
		}
		else if ( InheritanceType.SINGLE_TABLE.equals( inheritanceState.getType() ) ) {
			if ( isInheritanceRoot ) {
				if ( hasSubclasses || !discriminatorColumn.isImplicit() ) {
					bindDiscriminatorColumnToRootPersistentClass(
							(RootClass) persistentClass,
							discriminatorColumn,
							entityBinder.getSecondaryTables(),
							propertyHolder,
							context
					);
					//bind it again since the type might have changed
					entityBinder.bindDiscriminatorValue();
				}
			}
		}

		if ( onDeleteAnn != null && !onDeleteAppropriate ) {
			LOG.invalidOnDeleteAnnotation(propertyHolder.getEntityName());
		}

		// try to find class level generators
		HashMap<String, IdentifierGeneratorDefinition> classGenerators = buildLocalGenerators( clazzToProcess, context );

		// check properties
		final InheritanceState.ElementsToProcess elementsToProcess = inheritanceState.getElementsToProcess();
		inheritanceState.postProcess( persistentClass, entityBinder );

		final boolean subclassAndSingleTableStrategy = inheritanceState.getType() == InheritanceType.SINGLE_TABLE
				&& inheritanceState.hasParents();
		Set<String> idPropertiesIfIdClass = new HashSet<String>();
		boolean isIdClass = mapAsIdClass(
				inheritanceStatePerClass,
				inheritanceState,
				persistentClass,
				entityBinder,
				propertyHolder,
				elementsToProcess,
				idPropertiesIfIdClass,
				context
		);

		if ( !isIdClass ) {
			entityBinder.setWrapIdsInEmbeddedComponents( elementsToProcess.getIdPropertyCount() > 1 );
		}

		processIdPropertiesIfNotAlready(
				inheritanceStatePerClass,
				context,
				persistentClass,
				entityBinder,
				propertyHolder,
				classGenerators,
				elementsToProcess,
				subclassAndSingleTableStrategy,
				idPropertiesIfIdClass
		);

		if ( !inheritanceState.hasParents() ) {
			final RootClass rootClass = ( RootClass ) persistentClass;
			context.getMetadataCollector().addSecondPass( new CreateKeySecondPass( rootClass ) );
		}
		else {
			superEntity.addSubclass( ( Subclass ) persistentClass );
		}

		context.getMetadataCollector().addEntityBinding( persistentClass );

		//Process secondary tables and complementary definitions (ie o.h.a.Table)
		context.getMetadataCollector().addSecondPass(
				new SecondaryTableSecondPass(
						entityBinder,
						propertyHolder,
						clazzToProcess
				)
		);

		//add process complementary Table definition (index & all)
		entityBinder.processComplementaryTableDefinitions( clazzToProcess.getAnnotation( org.hibernate.annotations.Table.class ) );
		entityBinder.processComplementaryTableDefinitions( clazzToProcess.getAnnotation( org.hibernate.annotations.Tables.class ) );
		entityBinder.processComplementaryTableDefinitions( tabAnn );
	}

	/**
	 * Process all discriminator-related metadata per rules for "single table" inheritance
	 */
	private static Ejb3DiscriminatorColumn processSingleTableDiscriminatorProperties(
			XClass clazzToProcess,
			MetadataBuildingContext context,
			InheritanceState inheritanceState,
			EntityBinder entityBinder) {
		final boolean isRoot = !inheritanceState.hasParents();

		Ejb3DiscriminatorColumn discriminatorColumn = null;
		javax.persistence.DiscriminatorColumn discAnn = clazzToProcess.getAnnotation(
				javax.persistence.DiscriminatorColumn.class
		);
		DiscriminatorType discriminatorType = discAnn != null
				? discAnn.discriminatorType()
				: DiscriminatorType.STRING;

		org.hibernate.annotations.DiscriminatorFormula discFormulaAnn = clazzToProcess.getAnnotation(
				org.hibernate.annotations.DiscriminatorFormula.class
		);
		if ( isRoot ) {
			discriminatorColumn = Ejb3DiscriminatorColumn.buildDiscriminatorColumn(
					discriminatorType,
					discAnn,
					discFormulaAnn,
					context
			);
		}
		if ( discAnn != null && !isRoot ) {
			LOG.invalidDiscriminatorAnnotation( clazzToProcess.getName() );
		}

		final String discriminatorValue = clazzToProcess.isAnnotationPresent( DiscriminatorValue.class )
				? clazzToProcess.getAnnotation( DiscriminatorValue.class ).value()
				: null;
		entityBinder.setDiscriminatorValue( discriminatorValue );

		DiscriminatorOptions discriminatorOptions = clazzToProcess.getAnnotation( DiscriminatorOptions.class );
		if ( discriminatorOptions != null) {
			entityBinder.setForceDiscriminator( discriminatorOptions.force() );
			entityBinder.setInsertableDiscriminator( discriminatorOptions.insert() );
		}

		return discriminatorColumn;
	}

	/**
	 * Process all discriminator-related metadata per rules for "joined" inheritance
	 */
	private static Ejb3DiscriminatorColumn processJoinedDiscriminatorProperties(
			XClass clazzToProcess,
			MetadataBuildingContext context,
			InheritanceState inheritanceState,
			EntityBinder entityBinder) {
		if ( clazzToProcess.isAnnotationPresent( DiscriminatorFormula.class ) ) {
			throw new MappingException( "@DiscriminatorFormula on joined inheritance not supported at this time" );
		}


		// DiscriminatorValue handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final DiscriminatorValue discriminatorValueAnnotation = clazzToProcess.getAnnotation( DiscriminatorValue.class );
		final String discriminatorValue = discriminatorValueAnnotation != null
				? clazzToProcess.getAnnotation( DiscriminatorValue.class ).value()
				: null;
		entityBinder.setDiscriminatorValue( discriminatorValue );


		// DiscriminatorColumn handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final DiscriminatorColumn discriminatorColumnAnnotation = clazzToProcess.getAnnotation( DiscriminatorColumn.class );
		if ( !inheritanceState.hasParents() ) {
			// we want to process the discriminator column if either:
			//		1) There is an explicit DiscriminatorColumn annotation && we are not told to ignore them
			//		2) There is not an explicit DiscriminatorColumn annotation && we are told to create them implicitly
			final boolean generateDiscriminatorColumn;
			if ( discriminatorColumnAnnotation != null ) {
				if ( context.getBuildingOptions().ignoreExplicitDiscriminatorsForJoinedInheritance() ) {
					LOG.debugf( "Ignoring explicit DiscriminatorColumn annotation on ", clazzToProcess.getName() );
					generateDiscriminatorColumn = false;
				}
				else {
					LOG.applyingExplicitDiscriminatorColumnForJoined(
							clazzToProcess.getName(),
							AvailableSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
					);
					generateDiscriminatorColumn = true;
				}
			}
			else {
				if ( context.getBuildingOptions().createImplicitDiscriminatorsForJoinedInheritance() ) {
					LOG.debug( "Applying implicit DiscriminatorColumn using DiscriminatorColumn defaults" );
					generateDiscriminatorColumn = true;
				}
				else {
					LOG.debug( "Ignoring implicit (absent) DiscriminatorColumn" );
					generateDiscriminatorColumn = false;
				}
			}

			if ( generateDiscriminatorColumn ) {
				final DiscriminatorType discriminatorType = discriminatorColumnAnnotation != null
						? discriminatorColumnAnnotation.discriminatorType()
						: DiscriminatorType.STRING;
				return Ejb3DiscriminatorColumn.buildDiscriminatorColumn(
						discriminatorType,
						discriminatorColumnAnnotation,
						null,
						context
				);
			}
		}
		else {
			if ( discriminatorColumnAnnotation != null ) {
				LOG.invalidDiscriminatorAnnotation( clazzToProcess.getName() );
			}
		}

		return null;
	}

	private static void processIdPropertiesIfNotAlready(
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext context,
			PersistentClass persistentClass,
			EntityBinder entityBinder,
			PropertyHolder propertyHolder,
			HashMap<String, IdentifierGeneratorDefinition> classGenerators,
			InheritanceState.ElementsToProcess elementsToProcess,
			boolean subclassAndSingleTableStrategy,
			Set<String> idPropertiesIfIdClass) {
		Set<String> missingIdProperties = new HashSet<String>( idPropertiesIfIdClass );
		for ( PropertyData propertyAnnotatedElement : elementsToProcess.getElements() ) {
			String propertyName = propertyAnnotatedElement.getPropertyName();
			if ( !idPropertiesIfIdClass.contains( propertyName ) ) {
				processElementAnnotations(
						propertyHolder,
						subclassAndSingleTableStrategy
								? Nullability.FORCED_NULL
								: Nullability.NO_CONSTRAINT,
						propertyAnnotatedElement,
						classGenerators,
						entityBinder,
						false,
						false,
						false,
						context,
						inheritanceStatePerClass
				);
			}
			else {
				missingIdProperties.remove( propertyName );
			}
		}

		if ( missingIdProperties.size() != 0 ) {
			StringBuilder missings = new StringBuilder();
			for ( String property : missingIdProperties ) {
				missings.append( property ).append( ", " );
			}
			throw new AnnotationException(
					"Unable to find properties ("
							+ missings.substring( 0, missings.length() - 2 )
							+ ") in entity annotated with @IdClass:" + persistentClass.getEntityName()
			);
		}
	}

	private static boolean mapAsIdClass(
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			InheritanceState inheritanceState,
			PersistentClass persistentClass,
			EntityBinder entityBinder,
			PropertyHolder propertyHolder,
			InheritanceState.ElementsToProcess elementsToProcess,
			Set<String> idPropertiesIfIdClass,
			MetadataBuildingContext context) {
		/*
		 * We are looking for @IdClass
		 * In general we map the id class as identifier using the mapping metadata of the main entity's properties
		 * and we create an identifier mapper containing the id properties of the main entity
		 *
		 * In JPA 2, there is a shortcut if the id class is the Pk of the associated class pointed to by the id
		 * it ought to be treated as an embedded and not a real IdClass (at least in the Hibernate's internal way
		 */
		XClass classWithIdClass = inheritanceState.getClassWithIdClass( false );
		if ( classWithIdClass != null ) {
			IdClass idClass = classWithIdClass.getAnnotation( IdClass.class );
			XClass compositeClass = context.getBuildingOptions().getReflectionManager().toXClass( idClass.value() );
			PropertyData inferredData = new PropertyPreloadedData(
					entityBinder.getPropertyAccessType(), "id", compositeClass
			);
			PropertyData baseInferredData = new PropertyPreloadedData(
					entityBinder.getPropertyAccessType(), "id", classWithIdClass
			);
			AccessType propertyAccessor = entityBinder.getPropertyAccessor( compositeClass );
			//In JPA 2, there is a shortcut if the IdClass is the Pk of the associated class pointed to by the id
			//it ought to be treated as an embedded and not a real IdClass (at least in the Hibernate's internal way
			final boolean isFakeIdClass = isIdClassPkOfTheAssociatedEntity(
					elementsToProcess,
					compositeClass,
					inferredData,
					baseInferredData,
					propertyAccessor,
					inheritanceStatePerClass,
					context
			);

			if ( isFakeIdClass ) {
				return false;
			}

			boolean isComponent = true;
			String generatorType = "assigned";
			String generator = BinderHelper.ANNOTATION_STRING_DEFAULT;

			boolean ignoreIdAnnotations = entityBinder.isIgnoreIdAnnotations();
			entityBinder.setIgnoreIdAnnotations( true );
			propertyHolder.setInIdClass( true );
			bindIdClass(
					generatorType,
					generator,
					inferredData,
					baseInferredData,
					null,
					propertyHolder,
					isComponent,
					propertyAccessor,
					entityBinder,
					true,
					false,
					context,
					inheritanceStatePerClass
			);
			propertyHolder.setInIdClass( null );
			inferredData = new PropertyPreloadedData(
					propertyAccessor, PropertyPath.IDENTIFIER_MAPPER_PROPERTY, compositeClass
			);
			Component mapper = fillComponent(
					propertyHolder,
					inferredData,
					baseInferredData,
					propertyAccessor,
					false,
					entityBinder,
					true,
					true,
					false,
					context,
					inheritanceStatePerClass
			);
			entityBinder.setIgnoreIdAnnotations( ignoreIdAnnotations );
			persistentClass.setIdentifierMapper( mapper );

			//If id definition is on a mapped superclass, update the mapping
			final org.hibernate.mapping.MappedSuperclass superclass = BinderHelper.getMappedSuperclassOrNull(
					classWithIdClass,
					inheritanceStatePerClass,
					context
			);
			if ( superclass != null ) {
				superclass.setDeclaredIdentifierMapper( mapper );
			}
			else {
				//we are for sure on the entity
				persistentClass.setDeclaredIdentifierMapper( mapper );
			}

			Property property = new Property();
			property.setName( PropertyPath.IDENTIFIER_MAPPER_PROPERTY );
			property.setUpdateable( false );
			property.setInsertable( false );
			property.setValue( mapper );
			property.setPropertyAccessorName( "embedded" );
			persistentClass.addProperty( property );
			entityBinder.setIgnoreIdAnnotations( true );

			Iterator properties = mapper.getPropertyIterator();
			while ( properties.hasNext() ) {
				idPropertiesIfIdClass.add( ( ( Property ) properties.next() ).getName() );
			}
			return true;
		}
		else {
			return false;
		}
	}

	private static boolean isIdClassPkOfTheAssociatedEntity(
			InheritanceState.ElementsToProcess elementsToProcess,
			XClass compositeClass,
			PropertyData inferredData,
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext context) {
		if ( elementsToProcess.getIdPropertyCount() == 1 ) {
			final PropertyData idPropertyOnBaseClass = getUniqueIdPropertyFromBaseClass(
					inferredData,
					baseInferredData,
					propertyAccessor,
					context
			);
			final InheritanceState state = inheritanceStatePerClass.get( idPropertyOnBaseClass.getClassOrElement() );
			if ( state == null ) {
				return false; //while it is likely a user error, let's consider it is something that might happen
			}
			final XClass associatedClassWithIdClass = state.getClassWithIdClass( true );
			if ( associatedClassWithIdClass == null ) {
				//we cannot know for sure here unless we try and find the @EmbeddedId
				//Let's not do this thorough checking but do some extra validation
				final XProperty property = idPropertyOnBaseClass.getProperty();
				return property.isAnnotationPresent( ManyToOne.class )
						|| property.isAnnotationPresent( OneToOne.class );

			}
			else {
				final XClass idClass = context.getBuildingOptions().getReflectionManager().toXClass(
						associatedClassWithIdClass.getAnnotation( IdClass.class ).value()
				);
				return idClass.equals( compositeClass );
			}
		}
		else {
			return false;
		}
	}

	private static Cache determineCacheSettings(XClass clazzToProcess, MetadataBuildingContext context) {
		Cache cacheAnn = clazzToProcess.getAnnotation( Cache.class );
		if ( cacheAnn != null ) {
			return cacheAnn;
		}

		Cacheable cacheableAnn = clazzToProcess.getAnnotation( Cacheable.class );
		SharedCacheMode mode = determineSharedCacheMode( context );
		switch ( mode ) {
			case ALL: {
				cacheAnn = buildCacheMock( clazzToProcess.getName(), context );
				break;
			}
			case ENABLE_SELECTIVE: {
				if ( cacheableAnn != null && cacheableAnn.value() ) {
					cacheAnn = buildCacheMock( clazzToProcess.getName(), context );
				}
				break;
			}
			case DISABLE_SELECTIVE: {
				if ( cacheableAnn == null || cacheableAnn.value() ) {
					cacheAnn = buildCacheMock( clazzToProcess.getName(), context );
				}
				break;
			}
			default: {
				// treat both NONE and UNSPECIFIED the same
				break;
			}
		}
		return cacheAnn;
	}

	private static SharedCacheMode determineSharedCacheMode(MetadataBuildingContext context) {
		return context.getBuildingOptions().getSharedCacheMode();
	}

	private static Cache buildCacheMock(String region, MetadataBuildingContext context) {
		return new LocalCacheAnnotationImpl( region, determineCacheConcurrencyStrategy( context ) );
	}

	private static CacheConcurrencyStrategy DEFAULT_CACHE_CONCURRENCY_STRATEGY;

	private static CacheConcurrencyStrategy determineCacheConcurrencyStrategy(MetadataBuildingContext context) {
		if ( DEFAULT_CACHE_CONCURRENCY_STRATEGY == null ) {
			DEFAULT_CACHE_CONCURRENCY_STRATEGY = CacheConcurrencyStrategy.fromAccessType(
					context.getBuildingOptions().getImplicitCacheAccessType()
			);
		}
		return DEFAULT_CACHE_CONCURRENCY_STRATEGY;
	}

	@SuppressWarnings({ "ClassExplicitlyAnnotation" })
	private static class LocalCacheAnnotationImpl implements Cache {
		private final String region;
		private final CacheConcurrencyStrategy usage;

		private LocalCacheAnnotationImpl(String region, CacheConcurrencyStrategy usage) {
			this.region = region;
			this.usage = usage;
		}

		public CacheConcurrencyStrategy usage() {
			return usage;
		}

		public String region() {
			return region;
		}

		public String include() {
			return "all";
		}

		public Class<? extends Annotation> annotationType() {
			return Cache.class;
		}
	}

	private static PersistentClass makePersistentClass(
			InheritanceState inheritanceState,
			PersistentClass superEntity,
			MetadataBuildingContext metadataBuildingContext) {
		//we now know what kind of persistent entity it is
		if ( !inheritanceState.hasParents() ) {
			return new RootClass( metadataBuildingContext );
		}
		else if ( InheritanceType.SINGLE_TABLE.equals( inheritanceState.getType() ) ) {
			return new SingleTableSubclass( superEntity, metadataBuildingContext );
		}
		else if ( InheritanceType.JOINED.equals( inheritanceState.getType() ) ) {
			return new JoinedSubclass( superEntity, metadataBuildingContext );
		}
		else if ( InheritanceType.TABLE_PER_CLASS.equals( inheritanceState.getType() ) ) {
			return new UnionSubclass( superEntity, metadataBuildingContext );
		}
		else {
			throw new AssertionFailure( "Unknown inheritance type: " + inheritanceState.getType() );
		}
	}

	private static Ejb3JoinColumn[] makeInheritanceJoinColumns(
			XClass clazzToProcess,
			MetadataBuildingContext context,
			InheritanceState inheritanceState,
			PersistentClass superEntity) {
		Ejb3JoinColumn[] inheritanceJoinedColumns = null;
		final boolean hasJoinedColumns = inheritanceState.hasParents()
				&& InheritanceType.JOINED.equals( inheritanceState.getType() );
		if ( hasJoinedColumns ) {
			//@Inheritance(JOINED) subclass need to link back to the super entity
			PrimaryKeyJoinColumns jcsAnn = clazzToProcess.getAnnotation( PrimaryKeyJoinColumns.class );
			boolean explicitInheritanceJoinedColumns = jcsAnn != null && jcsAnn.value().length != 0;
			if ( explicitInheritanceJoinedColumns ) {
				int nbrOfInhJoinedColumns = jcsAnn.value().length;
				PrimaryKeyJoinColumn jcAnn;
				inheritanceJoinedColumns = new Ejb3JoinColumn[nbrOfInhJoinedColumns];
				for ( int colIndex = 0; colIndex < nbrOfInhJoinedColumns; colIndex++ ) {
					jcAnn = jcsAnn.value()[colIndex];
					inheritanceJoinedColumns[colIndex] = Ejb3JoinColumn.buildJoinColumn(
							jcAnn,
							null,
							superEntity.getIdentifier(),
							null,
							null,
							context
					);
				}
			}
			else {
				PrimaryKeyJoinColumn jcAnn = clazzToProcess.getAnnotation( PrimaryKeyJoinColumn.class );
				inheritanceJoinedColumns = new Ejb3JoinColumn[1];
				inheritanceJoinedColumns[0] = Ejb3JoinColumn.buildJoinColumn(
						jcAnn,
						null,
						superEntity.getIdentifier(),
						null,
						null,
						context
				);
			}
			LOG.trace( "Subclass joined column(s) created" );
		}
		else {
			if ( clazzToProcess.isAnnotationPresent( PrimaryKeyJoinColumns.class )
					|| clazzToProcess.isAnnotationPresent( PrimaryKeyJoinColumn.class ) ) {
				LOG.invalidPrimaryKeyJoinColumnAnnotation( clazzToProcess.getName() );
			}
		}
		return inheritanceJoinedColumns;
	}

	private static PersistentClass getSuperEntity(
			XClass clazzToProcess,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext context,
			InheritanceState inheritanceState) {
		InheritanceState superEntityState = InheritanceState.getInheritanceStateOfSuperEntity(
				clazzToProcess, inheritanceStatePerClass
		);
		PersistentClass superEntity = superEntityState != null
				? context.getMetadataCollector().getEntityBinding( superEntityState.getClazz().getName() )
				: null;
		if ( superEntity == null ) {
			//check if superclass is not a potential persistent class
			if ( inheritanceState.hasParents() ) {
				throw new AssertionFailure(
						"Subclass has to be binded afterQuery it's mother class: "
								+ superEntityState.getClazz().getName()
				);
			}
		}
		return superEntity;
	}

	private static boolean isEntityClassType(XClass clazzToProcess, AnnotatedClassType classType) {
		if ( AnnotatedClassType.EMBEDDABLE_SUPERCLASS.equals( classType ) //will be processed by their subentities
				|| AnnotatedClassType.NONE.equals( classType ) //to be ignored
				|| AnnotatedClassType.EMBEDDABLE.equals( classType ) //allow embeddable element declaration
				) {
			if ( AnnotatedClassType.NONE.equals( classType )
					&& clazzToProcess.isAnnotationPresent( org.hibernate.annotations.Entity.class ) ) {
				LOG.missingEntityAnnotation( clazzToProcess.getName() );
			}
			return false;
		}

		if ( !classType.equals( AnnotatedClassType.ENTITY ) ) {
			throw new AnnotationException(
					"Annotated class should have a @javax.persistence.Entity, @javax.persistence.Embeddable or @javax.persistence.EmbeddedSuperclass annotation: " + clazzToProcess
							.getName()
			);
		}

		return true;
	}

	/*
	 * Process the filters defined on the given class, as well as all filters defined
	 * on the MappedSuperclass(s) in the inheritance hierarchy
	 */

	private static void bindFilters(
			XClass annotatedClass,
			EntityBinder entityBinder,
			MetadataBuildingContext context) {

		bindFilters( annotatedClass, entityBinder );

		XClass classToProcess = annotatedClass.getSuperclass();
		while ( classToProcess != null ) {
			AnnotatedClassType classType = context.getMetadataCollector().getClassType( classToProcess );
			if ( AnnotatedClassType.EMBEDDABLE_SUPERCLASS.equals( classType ) ) {
				bindFilters( classToProcess, entityBinder );
			}
			else {
				break;
			}
			classToProcess = classToProcess.getSuperclass();
		}

	}

	private static void bindFilters(XAnnotatedElement annotatedElement, EntityBinder entityBinder) {

		Filters filtersAnn = annotatedElement.getAnnotation( Filters.class );
		if ( filtersAnn != null ) {
			for ( Filter filter : filtersAnn.value() ) {
				entityBinder.addFilter(filter);
			}
		}

		Filter filterAnn = annotatedElement.getAnnotation( Filter.class );
		if ( filterAnn != null ) {
			entityBinder.addFilter(filterAnn);
		}
	}

	private static void bindFilterDefs(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		FilterDef defAnn = annotatedElement.getAnnotation( FilterDef.class );
		FilterDefs defsAnn = annotatedElement.getAnnotation( FilterDefs.class );
		if ( defAnn != null ) {
			bindFilterDef( defAnn, context );
		}
		if ( defsAnn != null ) {
			for ( FilterDef def : defsAnn.value() ) {
				bindFilterDef( def, context );
			}
		}
	}

	private static void bindFilterDef(FilterDef defAnn, MetadataBuildingContext context) {
		Map<String, org.hibernate.type.Type> params = new HashMap<String, org.hibernate.type.Type>();
		for ( ParamDef param : defAnn.parameters() ) {
			params.put( param.name(), context.getMetadataCollector().getTypeResolver().heuristicType( param.type() ) );
		}
		FilterDefinition def = new FilterDefinition( defAnn.name(), defAnn.defaultCondition(), params );
		LOG.debugf( "Binding filter definition: %s", def.getFilterName() );
		context.getMetadataCollector().addFilterDefinition( def );
	}

	private static void bindTypeDefs(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		TypeDef defAnn = annotatedElement.getAnnotation( TypeDef.class );
		TypeDefs defsAnn = annotatedElement.getAnnotation( TypeDefs.class );
		if ( defAnn != null ) {
			bindTypeDef( defAnn, context );
		}
		if ( defsAnn != null ) {
			for ( TypeDef def : defsAnn.value() ) {
				bindTypeDef( def, context );
			}
		}
	}

	private static void bindTypeDef(TypeDef defAnn, MetadataBuildingContext context) {
		Properties params = new Properties();
		for ( Parameter param : defAnn.parameters() ) {
			params.setProperty( param.name(), param.value() );
		}

		if ( BinderHelper.isEmptyAnnotationValue( defAnn.name() ) && defAnn.defaultForType().equals( void.class ) ) {
			throw new AnnotationException(
					"Either name or defaultForType (or both) attribute should be set in TypeDef having typeClass " +
							defAnn.typeClass().getName()
			);
		}

		final String typeBindMessageF = "Binding type definition: %s";
		if ( !BinderHelper.isEmptyAnnotationValue( defAnn.name() ) ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( typeBindMessageF, defAnn.name() );
			}
			context.getMetadataCollector().addTypeDefinition(
					new TypeDefinition(
							defAnn.name(),
							defAnn.typeClass(),
							null,
							params
					)
			);
		}

		if ( !defAnn.defaultForType().equals( void.class ) ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( typeBindMessageF, defAnn.defaultForType().getName() );
			}
			context.getMetadataCollector().addTypeDefinition(
					new TypeDefinition(
							defAnn.defaultForType().getName(),
							defAnn.typeClass(),
							new String[]{ defAnn.defaultForType().getName() },
							params
					)
			);
		}

	}

	private static void bindFetchProfiles(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		FetchProfile fetchProfileAnnotation = annotatedElement.getAnnotation( FetchProfile.class );
		FetchProfiles fetchProfileAnnotations = annotatedElement.getAnnotation( FetchProfiles.class );
		if ( fetchProfileAnnotation != null ) {
			bindFetchProfile( fetchProfileAnnotation, context );
		}
		if ( fetchProfileAnnotations != null ) {
			for ( FetchProfile profile : fetchProfileAnnotations.value() ) {
				bindFetchProfile( profile, context );
			}
		}
	}

	private static void bindFetchProfile(FetchProfile fetchProfileAnnotation, MetadataBuildingContext context) {
		for ( FetchProfile.FetchOverride fetch : fetchProfileAnnotation.fetchOverrides() ) {
			org.hibernate.annotations.FetchMode mode = fetch.mode();
			if ( !mode.equals( org.hibernate.annotations.FetchMode.JOIN ) ) {
				throw new MappingException( "Only FetchMode.JOIN is currently supported" );
			}

			context.getMetadataCollector().addSecondPass(
					new VerifyFetchProfileReferenceSecondPass(
							fetchProfileAnnotation.name(),
							fetch,
							context
					)
			);
		}
	}


	private static void bindDiscriminatorColumnToRootPersistentClass(
			RootClass rootClass,
			Ejb3DiscriminatorColumn discriminatorColumn,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context) {
		if ( rootClass.getDiscriminator() == null ) {
			if ( discriminatorColumn == null ) {
				throw new AssertionFailure( "discriminator column should have been built" );
			}
			discriminatorColumn.setJoins( secondaryTables );
			discriminatorColumn.setPropertyHolder( propertyHolder );
			SimpleValue discriminatorColumnBinding = new SimpleValue( context.getMetadataCollector(), rootClass.getTable() );
			rootClass.setDiscriminator( discriminatorColumnBinding );
			discriminatorColumn.linkWithValue( discriminatorColumnBinding );
			discriminatorColumnBinding.setTypeName( discriminatorColumn.getDiscriminatorTypeName() );
			rootClass.setPolymorphic( true );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Setting discriminator for entity {0}", rootClass.getEntityName() );
			}
		}
	}

	/**
	 * @param elements List of {@code ProperyData} instances
	 * @param propertyContainer Metadata about a class and its properties
	 *
	 * @return the number of id properties found while iterating the elements of {@code annotatedClass} using
	 *         the determined access strategy, {@code false} otherwise.
	 */
	static int addElementsOfClass(
			List<PropertyData> elements,
			PropertyContainer propertyContainer,
			MetadataBuildingContext context) {
		int idPropertyCounter = 0;

		Collection<XProperty> properties = propertyContainer.getProperties();
		for ( XProperty p : properties ) {
			final int currentIdPropertyCounter = addProperty(
					propertyContainer,
					p,
					elements,
					context
			);
			idPropertyCounter += currentIdPropertyCounter;
		}
		return idPropertyCounter;
	}

	private static int addProperty(
			PropertyContainer propertyContainer,
			XProperty property,
			List<PropertyData> inFlightPropertyDataList,
			MetadataBuildingContext context) {
		// see if inFlightPropertyDataList already contains a PropertyData for this name,
		// and if so, skip it..
		for ( PropertyData propertyData : inFlightPropertyDataList ) {
			if ( propertyData.getPropertyName().equals( property.getName() ) ) {
				// EARLY EXIT!!!
				return 0;
			}
		}

		final XClass declaringClass = propertyContainer.getDeclaringClass();
		final XClass entity = propertyContainer.getEntityAtStake();
		int idPropertyCounter = 0;
		PropertyData propertyAnnotatedElement = new PropertyInferredData(
				declaringClass,
				property,
				propertyContainer.getClassLevelAccessType().getType(),
				context.getBuildingOptions().getReflectionManager()
		);

		/*
		 * put element annotated by @Id in front
		 * since it has to be parsed beforeQuery any association by Hibernate
		 */
		final XAnnotatedElement element = propertyAnnotatedElement.getProperty();
		if ( element.isAnnotationPresent( Id.class ) || element.isAnnotationPresent( EmbeddedId.class ) ) {
			inFlightPropertyDataList.add( 0, propertyAnnotatedElement );
			/**
			 * The property must be put in hibernate.properties as it's a system wide property. Fixable?
			 * TODO support true/false/default on the property instead of present / not present
			 * TODO is @Column mandatory?
			 * TODO add method support
			 */
			if ( context.getBuildingOptions().isSpecjProprietarySyntaxEnabled() ) {
				if ( element.isAnnotationPresent( Id.class ) && element.isAnnotationPresent( Column.class ) ) {
					String columnName = element.getAnnotation( Column.class ).name();
					for ( XProperty prop : declaringClass.getDeclaredProperties( AccessType.FIELD.getType() ) ) {
						if ( !prop.isAnnotationPresent( MapsId.class ) ) {
							/**
							 * The detection of a configured individual JoinColumn differs between Annotation
							 * and XML configuration processing.
							 */
							boolean isRequiredAnnotationPresent = false;
							JoinColumns groupAnnotation = prop.getAnnotation( JoinColumns.class );
							if ( (prop.isAnnotationPresent( JoinColumn.class )
									&& prop.getAnnotation( JoinColumn.class ).name().equals( columnName )) ) {
								isRequiredAnnotationPresent = true;
							}
							else if ( prop.isAnnotationPresent( JoinColumns.class ) ) {
								for ( JoinColumn columnAnnotation : groupAnnotation.value() ) {
									if ( columnName.equals( columnAnnotation.name() ) ) {
										isRequiredAnnotationPresent = true;
										break;
									}
								}
							}
							if ( isRequiredAnnotationPresent ) {
								//create a PropertyData fpr the specJ property holding the mapping
								PropertyData specJPropertyData = new PropertyInferredData(
										declaringClass,
										//same dec
										prop,
										// the actual @XToOne property
										propertyContainer.getClassLevelAccessType().getType(),
										//TODO we should get the right accessor but the same as id would do
										context.getBuildingOptions().getReflectionManager()
								);
								context.getMetadataCollector().addPropertyAnnotatedWithMapsIdSpecj(
										entity,
										specJPropertyData,
										element.toString()
								);
							}
						}
					}
				}
			}

			if ( element.isAnnotationPresent( ManyToOne.class ) || element.isAnnotationPresent( OneToOne.class ) ) {
				context.getMetadataCollector().addToOneAndIdProperty( entity, propertyAnnotatedElement );
			}
			idPropertyCounter++;
		}
		else {
			inFlightPropertyDataList.add( propertyAnnotatedElement );
		}
		if ( element.isAnnotationPresent( MapsId.class ) ) {
			context.getMetadataCollector().addPropertyAnnotatedWithMapsId( entity, propertyAnnotatedElement );
		}

		return idPropertyCounter;
	}

	/*
	 * Process annotation of a particular property
	 */

	private static void processElementAnnotations(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			HashMap<String, IdentifierGeneratorDefinition> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) throws MappingException {

		if ( !propertyHolder.isComponent() ) {
			if ( entityBinder.isPropertyDefinedInSuperHierarchy( inferredData.getPropertyName() ) ) {
				LOG.debugf(
						"Skipping attribute [%s : %s] as it was already processed as part of super hierarchy",
						inferredData.getClassOrElementName(),
						inferredData.getPropertyName()
				);
				return;
			}
		}

		/**
		 * inSecondPass can only be used to apply right away the second pass of a composite-element
		 * Because it's a value type, there is no bidirectional association, hence second pass
		 * ordering does not matter
		 */

		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( traceEnabled ) {
			LOG.tracev( "Processing annotations of {0}.{1}" , propertyHolder.getEntityName(), inferredData.getPropertyName() );
		}

		final XProperty property = inferredData.getProperty();
		if ( property.isAnnotationPresent( Parent.class ) ) {
			if ( propertyHolder.isComponent() ) {
				propertyHolder.setParentProperty( property.getName() );
			}
			else {
				throw new AnnotationException(
						"@Parent cannot be applied outside an embeddable object: "
								+ BinderHelper.getPath( propertyHolder, inferredData )
				);
			}
			return;
		}

		ColumnsBuilder columnsBuilder = new ColumnsBuilder(
				propertyHolder,
				nullability,
				property,
				inferredData,
				entityBinder,
				context
		).extractMetadata();
		Ejb3Column[] columns = columnsBuilder.getColumns();
		Ejb3JoinColumn[] joinColumns = columnsBuilder.getJoinColumns();

		final XClass returnedClass = inferredData.getClassOrElement();

		//prepare PropertyBinder
		PropertyBinder propertyBinder = new PropertyBinder();
		propertyBinder.setName( inferredData.getPropertyName() );
		propertyBinder.setReturnedClassName( inferredData.getTypeName() );
		propertyBinder.setAccessType( inferredData.getDefaultAccess() );
		propertyBinder.setHolder( propertyHolder );
		propertyBinder.setProperty( property );
		propertyBinder.setReturnedClass( inferredData.getPropertyClass() );
		propertyBinder.setBuildingContext( context );
		if ( isIdentifierMapper ) {
			propertyBinder.setInsertable( false );
			propertyBinder.setUpdatable( false );
		}
		propertyBinder.setDeclaringClass( inferredData.getDeclaringClass() );
		propertyBinder.setEntityBinder( entityBinder );
		propertyBinder.setInheritanceStatePerClass( inheritanceStatePerClass );

		boolean isId = !entityBinder.isIgnoreIdAnnotations() &&
				( property.isAnnotationPresent( Id.class )
						|| property.isAnnotationPresent( EmbeddedId.class ) );
		propertyBinder.setId( isId );

		final LazyGroup lazyGroupAnnotation = property.getAnnotation( LazyGroup.class );
		if ( lazyGroupAnnotation != null ) {
			propertyBinder.setLazyGroup( lazyGroupAnnotation.value() );
		}

		if ( property.isAnnotationPresent( Version.class ) ) {
			if ( isIdentifierMapper ) {
				throw new AnnotationException(
						"@IdClass class should not have @Version property"
				);
			}
			if ( !( propertyHolder.getPersistentClass() instanceof RootClass ) ) {
				throw new AnnotationException(
						"Unable to define/override @Version on a subclass: "
								+ propertyHolder.getEntityName()
				);
			}
			if ( !propertyHolder.isEntity() ) {
				throw new AnnotationException(
						"Unable to define @Version on an embedded class: "
								+ propertyHolder.getEntityName()
				);
			}
			if ( traceEnabled ) {
				LOG.tracev( "{0} is a version property", inferredData.getPropertyName() );
			}
			RootClass rootClass = ( RootClass ) propertyHolder.getPersistentClass();
			propertyBinder.setColumns( columns );
			Property prop = propertyBinder.makePropertyValueAndBind();
			setVersionInformation( property, propertyBinder );
			rootClass.setVersion( prop );

			//If version is on a mapped superclass, update the mapping
			final org.hibernate.mapping.MappedSuperclass superclass = BinderHelper.getMappedSuperclassOrNull(
					inferredData.getDeclaringClass(),
					inheritanceStatePerClass,
					context
			);
			if ( superclass != null ) {
				superclass.setDeclaredVersion( prop );
			}
			else {
				//we know the property is on the actual entity
				rootClass.setDeclaredVersion( prop );
			}

			SimpleValue simpleValue = ( SimpleValue ) prop.getValue();
			simpleValue.setNullValue( "undefined" );
			rootClass.setOptimisticLockStyle( OptimisticLockStyle.VERSION );
			if ( traceEnabled ) {
				LOG.tracev( "Version name: {0}, unsavedValue: {1}", rootClass.getVersion().getName(),
						( (SimpleValue) rootClass.getVersion().getValue() ).getNullValue() );
			}
		}
		else {
			final boolean forcePersist = property.isAnnotationPresent( MapsId.class )
					|| property.isAnnotationPresent( Id.class );
			if ( property.isAnnotationPresent( ManyToOne.class ) ) {
				ManyToOne ann = property.getAnnotation( ManyToOne.class );

				//check validity
				if ( property.isAnnotationPresent( Column.class )
						|| property.isAnnotationPresent( Columns.class ) ) {
					throw new AnnotationException(
							"@Column(s) not allowed on a @ManyToOne property: "
									+ BinderHelper.getPath( propertyHolder, inferredData )
					);
				}

				Cascade hibernateCascade = property.getAnnotation( Cascade.class );
				NotFound notFound = property.getAnnotation( NotFound.class );
				boolean ignoreNotFound = notFound != null && notFound.action().equals( NotFoundAction.IGNORE );
				OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
				boolean onDeleteCascade = onDeleteAnn != null && OnDeleteAction.CASCADE.equals( onDeleteAnn.action() );
				JoinTable assocTable = propertyHolder.getJoinTable( property );
				if ( assocTable != null ) {
					Join join = propertyHolder.addJoin( assocTable, false );
					for ( Ejb3JoinColumn joinColumn : joinColumns ) {
						joinColumn.setExplicitTableName( join.getTable().getName() );
					}
				}
				final boolean mandatory = !ann.optional() || forcePersist;
				bindManyToOne(
						getCascadeStrategy( ann.cascade(), hibernateCascade, false, forcePersist ),
						joinColumns,
						!mandatory,
						ignoreNotFound,
						onDeleteCascade,
						ToOneBinder.getTargetEntity( inferredData, context ),
						propertyHolder,
						inferredData,
						false,
						isIdentifierMapper,
						inSecondPass,
						propertyBinder,
						context
				);
			}
			else if ( property.isAnnotationPresent( OneToOne.class ) ) {
				OneToOne ann = property.getAnnotation( OneToOne.class );

				//check validity
				if ( property.isAnnotationPresent( Column.class )
						|| property.isAnnotationPresent( Columns.class ) ) {
					throw new AnnotationException(
							"@Column(s) not allowed on a @OneToOne property: "
									+ BinderHelper.getPath( propertyHolder, inferredData )
					);
				}

				//FIXME support a proper PKJCs
				boolean trueOneToOne = property.isAnnotationPresent( PrimaryKeyJoinColumn.class )
						|| property.isAnnotationPresent( PrimaryKeyJoinColumns.class );
				Cascade hibernateCascade = property.getAnnotation( Cascade.class );
				NotFound notFound = property.getAnnotation( NotFound.class );
				boolean ignoreNotFound = notFound != null && notFound.action().equals( NotFoundAction.IGNORE );
				OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
				boolean onDeleteCascade = onDeleteAnn != null && OnDeleteAction.CASCADE.equals( onDeleteAnn.action() );
				JoinTable assocTable = propertyHolder.getJoinTable( property );
				if ( assocTable != null ) {
					Join join = propertyHolder.addJoin( assocTable, false );
					for ( Ejb3JoinColumn joinColumn : joinColumns ) {
						joinColumn.setExplicitTableName( join.getTable().getName() );
					}
				}
				//MapsId means the columns belong to the pk => not null
				//@OneToOne with @PKJC can still be optional
				final boolean mandatory = !ann.optional() || forcePersist;
				bindOneToOne(
						getCascadeStrategy( ann.cascade(), hibernateCascade, ann.orphanRemoval(), forcePersist ),
						joinColumns,
						!mandatory,
						getFetchMode( ann.fetch() ),
						ignoreNotFound, onDeleteCascade,
						ToOneBinder.getTargetEntity( inferredData, context ),
						propertyHolder,
						inferredData,
						ann.mappedBy(),
						trueOneToOne,
						isIdentifierMapper,
						inSecondPass,
						propertyBinder,
						context
				);
			}
			else if ( property.isAnnotationPresent( org.hibernate.annotations.Any.class ) ) {

				//check validity
				if ( property.isAnnotationPresent( Column.class )
						|| property.isAnnotationPresent( Columns.class ) ) {
					throw new AnnotationException(
							"@Column(s) not allowed on a @Any property: "
									+ BinderHelper.getPath( propertyHolder, inferredData )
					);
				}

				Cascade hibernateCascade = property.getAnnotation( Cascade.class );
				OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
				boolean onDeleteCascade = onDeleteAnn != null && OnDeleteAction.CASCADE.equals( onDeleteAnn.action() );
				JoinTable assocTable = propertyHolder.getJoinTable( property );
				if ( assocTable != null ) {
					Join join = propertyHolder.addJoin( assocTable, false );
					for ( Ejb3JoinColumn joinColumn : joinColumns ) {
						joinColumn.setExplicitTableName( join.getTable().getName() );
					}
				}
				bindAny(
						getCascadeStrategy( null, hibernateCascade, false, forcePersist ),
						//@Any has not cascade attribute
						joinColumns,
						onDeleteCascade,
						nullability,
						propertyHolder,
						inferredData,
						entityBinder,
						isIdentifierMapper,
						context
				);
			}
			else if ( property.isAnnotationPresent( OneToMany.class )
					|| property.isAnnotationPresent( ManyToMany.class )
					|| property.isAnnotationPresent( ElementCollection.class )
					|| property.isAnnotationPresent( ManyToAny.class ) ) {
				OneToMany oneToManyAnn = property.getAnnotation( OneToMany.class );
				ManyToMany manyToManyAnn = property.getAnnotation( ManyToMany.class );
				ElementCollection elementCollectionAnn = property.getAnnotation( ElementCollection.class );

				if ( ( oneToManyAnn != null || manyToManyAnn != null || elementCollectionAnn != null ) &&
						isToManyAssociationWithinEmbeddableCollection(
								propertyHolder ) ) {
					throw new AnnotationException(
							"@OneToMany, @ManyToMany or @ElementCollection cannot be used inside an @Embeddable that is also contained within an @ElementCollection: "
									+ BinderHelper.getPath(
									propertyHolder,
									inferredData
							)
					);
				}

				final IndexColumn indexColumn;

				if ( property.isAnnotationPresent( OrderColumn.class ) ) {
					indexColumn = IndexColumn.buildColumnFromAnnotation(
							property.getAnnotation( OrderColumn.class ),
							propertyHolder,
							inferredData,
							entityBinder.getSecondaryTables(),
							context
					);
					if ( property.isAnnotationPresent( ListIndexBase.class ) ) {
						indexColumn.setBase( ( property.getAnnotation( ListIndexBase.class ) ).value() );
					}
				}
				else {
					//if @IndexColumn is not there, the generated IndexColumn is an implicit column and not used.
					//so we can leave the legacy processing as the default
					indexColumn = IndexColumn.buildColumnFromAnnotation(
							property.getAnnotation( org.hibernate.annotations.IndexColumn.class ),
							propertyHolder,
							inferredData,
							context
					);
				}
				CollectionBinder collectionBinder = CollectionBinder.getCollectionBinder(
						propertyHolder.getEntityName(),
						property,
						!indexColumn.isImplicit(),
						property.isAnnotationPresent( MapKeyType.class ),
						context
				);
				collectionBinder.setIndexColumn( indexColumn );
				collectionBinder.setMapKey( property.getAnnotation( MapKey.class ) );
				collectionBinder.setPropertyName( inferredData.getPropertyName() );

				collectionBinder.setBatchSize( property.getAnnotation( BatchSize.class ) );

				collectionBinder.setJpaOrderBy( property.getAnnotation( javax.persistence.OrderBy.class ) );
				collectionBinder.setSqlOrderBy( property.getAnnotation( OrderBy.class ) );

				collectionBinder.setSort( property.getAnnotation( Sort.class ) );
				collectionBinder.setNaturalSort( property.getAnnotation( SortNatural.class ) );
				collectionBinder.setComparatorSort( property.getAnnotation( SortComparator.class ) );

				Cache cachAnn = property.getAnnotation( Cache.class );
				collectionBinder.setCache( cachAnn );
				collectionBinder.setPropertyHolder( propertyHolder );
				Cascade hibernateCascade = property.getAnnotation( Cascade.class );
				NotFound notFound = property.getAnnotation( NotFound.class );
				boolean ignoreNotFound = notFound != null && notFound.action().equals( NotFoundAction.IGNORE );
				collectionBinder.setIgnoreNotFound( ignoreNotFound );
				collectionBinder.setCollectionType( inferredData.getProperty().getElementClass() );
				collectionBinder.setBuildingContext( context );
				collectionBinder.setAccessType( inferredData.getDefaultAccess() );

				Ejb3Column[] elementColumns;
				//do not use "element" if you are a JPA 2 @ElementCollection only for legacy Hibernate mappings
				boolean isJPA2ForValueMapping = property.isAnnotationPresent( ElementCollection.class );
				PropertyData virtualProperty = isJPA2ForValueMapping ? inferredData : new WrappedInferredData(
						inferredData, "element"
				);
				if ( property.isAnnotationPresent( Column.class ) || property.isAnnotationPresent(
						Formula.class
				) ) {
					Column ann = property.getAnnotation( Column.class );
					Formula formulaAnn = property.getAnnotation( Formula.class );
					elementColumns = Ejb3Column.buildColumnFromAnnotation(
							new Column[] { ann },
							formulaAnn,
							nullability,
							propertyHolder,
							virtualProperty,
							entityBinder.getSecondaryTables(),
							context
					);
				}
				else if ( property.isAnnotationPresent( Columns.class ) ) {
					Columns anns = property.getAnnotation( Columns.class );
					elementColumns = Ejb3Column.buildColumnFromAnnotation(
							anns.columns(),
							null,
							nullability,
							propertyHolder,
							virtualProperty,
							entityBinder.getSecondaryTables(),
							context
					);
				}
				else {
					elementColumns = Ejb3Column.buildColumnFromAnnotation(
							null,
							null,
							nullability,
							propertyHolder,
							virtualProperty,
							entityBinder.getSecondaryTables(),
							context
					);
				}
				{
					Column[] keyColumns = null;
					//JPA 2 has priority and has different default column values, differenciate legacy from JPA 2
					Boolean isJPA2 = null;
					if ( property.isAnnotationPresent( MapKeyColumn.class ) ) {
						isJPA2 = Boolean.TRUE;
						keyColumns = new Column[] { new MapKeyColumnDelegator( property.getAnnotation( MapKeyColumn.class ) ) };
					}

					//not explicitly legacy
					if ( isJPA2 == null ) {
						isJPA2 = Boolean.TRUE;
					}

					//nullify empty array
					keyColumns = keyColumns != null && keyColumns.length > 0 ? keyColumns : null;

					//"mapkey" is the legacy column name of the key column pre JPA 2
					PropertyData mapKeyVirtualProperty = new WrappedInferredData( inferredData, "mapkey" );
					Ejb3Column[] mapColumns = Ejb3Column.buildColumnFromAnnotation(
							keyColumns,
							null,
							Nullability.FORCED_NOT_NULL,
							propertyHolder,
							isJPA2 ? inferredData : mapKeyVirtualProperty,
							isJPA2 ? "_KEY" : null,
							entityBinder.getSecondaryTables(),
							context
					);
					collectionBinder.setMapKeyColumns( mapColumns );
				}
				{
					JoinColumn[] joinKeyColumns = null;
					//JPA 2 has priority and has different default column values, differenciate legacy from JPA 2
					Boolean isJPA2 = null;
					if ( property.isAnnotationPresent( MapKeyJoinColumns.class ) ) {
						isJPA2 = Boolean.TRUE;
						final MapKeyJoinColumn[] mapKeyJoinColumns = property.getAnnotation( MapKeyJoinColumns.class )
								.value();
						joinKeyColumns = new JoinColumn[mapKeyJoinColumns.length];
						int index = 0;
						for ( MapKeyJoinColumn joinColumn : mapKeyJoinColumns ) {
							joinKeyColumns[index] = new MapKeyJoinColumnDelegator( joinColumn );
							index++;
						}
						if ( property.isAnnotationPresent( MapKeyJoinColumn.class ) ) {
							throw new AnnotationException(
									"@MapKeyJoinColumn and @MapKeyJoinColumns used on the same property: "
											+ BinderHelper.getPath( propertyHolder, inferredData )
							);
						}
					}
					else if ( property.isAnnotationPresent( MapKeyJoinColumn.class ) ) {
						isJPA2 = Boolean.TRUE;
						joinKeyColumns = new JoinColumn[] {
								new MapKeyJoinColumnDelegator(
										property.getAnnotation(
												MapKeyJoinColumn.class
										)
								)
						};
					}
					//not explicitly legacy
					if ( isJPA2 == null ) {
						isJPA2 = Boolean.TRUE;
					}

					PropertyData mapKeyVirtualProperty = new WrappedInferredData( inferredData, "mapkey" );
					Ejb3JoinColumn[] mapJoinColumns = Ejb3JoinColumn.buildJoinColumnsWithDefaultColumnSuffix(
							joinKeyColumns,
							null,
							entityBinder.getSecondaryTables(),
							propertyHolder,
							isJPA2 ? inferredData.getPropertyName() : mapKeyVirtualProperty.getPropertyName(),
							isJPA2 ? "_KEY" : null,
							context
					);
					collectionBinder.setMapKeyManyToManyColumns( mapJoinColumns );
				}

				//potential element
				collectionBinder.setEmbedded( property.isAnnotationPresent( Embedded.class ) );
				collectionBinder.setElementColumns( elementColumns );
				collectionBinder.setProperty( property );

				//TODO enhance exception with @ManyToAny and @CollectionOfElements
				if ( oneToManyAnn != null && manyToManyAnn != null ) {
					throw new AnnotationException(
							"@OneToMany and @ManyToMany on the same property is not allowed: "
									+ propertyHolder.getEntityName() + "." + inferredData.getPropertyName()
					);
				}
				String mappedBy = null;
				if ( oneToManyAnn != null ) {
					for ( Ejb3JoinColumn column : joinColumns ) {
						if ( column.isSecondary() ) {
							throw new NotYetImplementedException( "Collections having FK in secondary table" );
						}
					}
					collectionBinder.setFkJoinColumns( joinColumns );
					mappedBy = oneToManyAnn.mappedBy();
					collectionBinder.setTargetEntity(
							context.getBuildingOptions().getReflectionManager().toXClass( oneToManyAnn.targetEntity() )
					);
					collectionBinder.setCascadeStrategy(
							getCascadeStrategy(
									oneToManyAnn.cascade(), hibernateCascade, oneToManyAnn.orphanRemoval(), false
							)
					);
					collectionBinder.setOneToMany( true );
				}
				else if ( elementCollectionAnn != null ) {
					for ( Ejb3JoinColumn column : joinColumns ) {
						if ( column.isSecondary() ) {
							throw new NotYetImplementedException( "Collections having FK in secondary table" );
						}
					}
					collectionBinder.setFkJoinColumns( joinColumns );
					mappedBy = "";
					final Class<?> targetElement = elementCollectionAnn.targetClass();
					collectionBinder.setTargetEntity(
							context.getBuildingOptions().getReflectionManager().toXClass( targetElement )
					);
					//collectionBinder.setCascadeStrategy( getCascadeStrategy( embeddedCollectionAnn.cascade(), hibernateCascade ) );
					collectionBinder.setOneToMany( true );
				}
				else if ( manyToManyAnn != null ) {
					mappedBy = manyToManyAnn.mappedBy();
					collectionBinder.setTargetEntity(
							context.getBuildingOptions().getReflectionManager().toXClass( manyToManyAnn.targetEntity() )
					);
					collectionBinder.setCascadeStrategy(
							getCascadeStrategy(
									manyToManyAnn.cascade(), hibernateCascade, false, false
							)
					);
					collectionBinder.setOneToMany( false );
				}
				else if ( property.isAnnotationPresent( ManyToAny.class ) ) {
					mappedBy = "";
					collectionBinder.setTargetEntity(
							context.getBuildingOptions().getReflectionManager().toXClass( void.class )
					);
					collectionBinder.setCascadeStrategy( getCascadeStrategy( null, hibernateCascade, false, false ) );
					collectionBinder.setOneToMany( false );
				}
				collectionBinder.setMappedBy( mappedBy );

				bindJoinedTableAssociation(
						property,
						context,
						entityBinder,
						collectionBinder,
						propertyHolder,
						inferredData,
						mappedBy
				);

				OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
				boolean onDeleteCascade = onDeleteAnn != null && OnDeleteAction.CASCADE.equals( onDeleteAnn.action() );
				collectionBinder.setCascadeDeleteEnabled( onDeleteCascade );
				if ( isIdentifierMapper ) {
					collectionBinder.setInsertable( false );
					collectionBinder.setUpdatable( false );
				}
				if ( property.isAnnotationPresent( CollectionId.class ) ) { //do not compute the generators unless necessary
					HashMap<String, IdentifierGeneratorDefinition> localGenerators = ( HashMap<String, IdentifierGeneratorDefinition> ) classGenerators.clone();
					localGenerators.putAll( buildLocalGenerators( property, context ) );
					collectionBinder.setLocalGenerators( localGenerators );

				}
				collectionBinder.setInheritanceStatePerClass( inheritanceStatePerClass );
				collectionBinder.setDeclaringClass( inferredData.getDeclaringClass() );
				collectionBinder.bind();

			}
			//Either a regular property or a basic @Id or @EmbeddedId while not ignoring id annotations
			else if ( !isId || !entityBinder.isIgnoreIdAnnotations() ) {
				//define whether the type is a component or not

				boolean isComponent = false;

				//Overrides from @MapsId if needed
				boolean isOverridden = false;
				if ( isId || propertyHolder.isOrWithinEmbeddedId() || propertyHolder.isInIdClass() ) {
					//the associated entity could be using an @IdClass making the overridden property a component
					final PropertyData overridingProperty = BinderHelper.getPropertyOverriddenByMapperOrMapsId(
							isId,
							propertyHolder,
							property.getName(),
							context
					);
					if ( overridingProperty != null ) {
						isOverridden = true;
						final InheritanceState state = inheritanceStatePerClass.get( overridingProperty.getClassOrElement() );
						if ( state != null ) {
							isComponent = isComponent || state.hasIdClassOrEmbeddedId();
						}
						//Get the new column
						columns = columnsBuilder.overrideColumnFromMapperOrMapsIdProperty( isId );
					}
				}

				isComponent = isComponent
						|| property.isAnnotationPresent( Embedded.class )
						|| property.isAnnotationPresent( EmbeddedId.class )
						|| returnedClass.isAnnotationPresent( Embeddable.class );


				if ( isComponent ) {
					String referencedEntityName = null;
					if ( isOverridden ) {
						final PropertyData mapsIdProperty = BinderHelper.getPropertyOverriddenByMapperOrMapsId(
								isId, propertyHolder, property.getName(), context
						);
						referencedEntityName = mapsIdProperty.getClassOrElementName();
					}
					AccessType propertyAccessor = entityBinder.getPropertyAccessor( property );
					propertyBinder = bindComponent(
							inferredData,
							propertyHolder,
							propertyAccessor,
							entityBinder,
							isIdentifierMapper,
							context,
							isComponentEmbedded,
							isId,
							inheritanceStatePerClass,
							referencedEntityName,
							isOverridden ? ( Ejb3JoinColumn[] ) columns : null
					);
				}
				else {
					//provide the basic property mapping
					boolean optional = true;
					boolean lazy = false;
					if ( property.isAnnotationPresent( Basic.class ) ) {
						Basic ann = property.getAnnotation( Basic.class );
						optional = ann.optional();
						lazy = ann.fetch() == FetchType.LAZY;
					}
					//implicit type will check basic types and Serializable classes
					if ( isId || ( !optional && nullability != Nullability.FORCED_NULL ) ) {
						//force columns to not null
						for ( Ejb3Column col : columns ) {
							if ( isId && col.isFormula() ) {
								throw new CannotForceNonNullableException(
										String.format(
												Locale.ROOT,
												"Identifier property [%s] cannot contain formula mapping [%s]",
												HCANNHelper.annotatedElementSignature( property ),
												col.getFormulaString()
										)
								);
							}
							col.forceNotNull();
						}
					}

					propertyBinder.setLazy( lazy );
					propertyBinder.setColumns( columns );
					if ( isOverridden ) {
						final PropertyData mapsIdProperty = BinderHelper.getPropertyOverriddenByMapperOrMapsId(
								isId, propertyHolder, property.getName(), context
						);
						propertyBinder.setReferencedEntityName( mapsIdProperty.getClassOrElementName() );
					}

					propertyBinder.makePropertyValueAndBind();

				}
				if ( isOverridden ) {
					final PropertyData mapsIdProperty = BinderHelper.getPropertyOverriddenByMapperOrMapsId(
							isId, propertyHolder, property.getName(), context
					);
					Map<String, IdentifierGeneratorDefinition> localGenerators = ( HashMap<String, IdentifierGeneratorDefinition> ) classGenerators.clone();
					final IdentifierGeneratorDefinition.Builder foreignGeneratorBuilder = new IdentifierGeneratorDefinition.Builder();
					foreignGeneratorBuilder.setName( "Hibernate-local--foreign generator" );
					foreignGeneratorBuilder.setStrategy( "foreign" );
					foreignGeneratorBuilder.addParam( "property", mapsIdProperty.getPropertyName() );

					final IdentifierGeneratorDefinition foreignGenerator = foreignGeneratorBuilder.build();
					localGenerators.put( foreignGenerator.getName(), foreignGenerator );

					BinderHelper.makeIdGenerator(
							( SimpleValue ) propertyBinder.getValue(),
							foreignGenerator.getStrategy(),
							foreignGenerator.getName(),
							context,
							localGenerators
					);
				}
				if ( isId ) {
					//components and regular basic types create SimpleValue objects
					final SimpleValue value = ( SimpleValue ) propertyBinder.getValue();
					if ( !isOverridden ) {
						processId(
								propertyHolder,
								inferredData,
								value,
								classGenerators,
								isIdentifierMapper,
								context
						);
					}
				}
			}
		}
		//init index
		//process indexes afterQuery everything: in second pass, many to one has to be done beforeQuery indexes
		Index index = property.getAnnotation( Index.class );
		if ( index != null ) {
			if ( joinColumns != null ) {

				for ( Ejb3Column column : joinColumns ) {
					column.addIndex( index, inSecondPass );
				}
			}
			else {
				if ( columns != null ) {
					for ( Ejb3Column column : columns ) {
						column.addIndex( index, inSecondPass );
					}
				}
			}
		}

		// Natural ID columns must reside in one single UniqueKey within the Table.
		// For now, simply ensure consistent naming.
		// TODO: AFAIK, there really isn't a reason for these UKs to be created
		// on the secondPass.  This whole area should go away...
		NaturalId naturalIdAnn = property.getAnnotation( NaturalId.class );
		if ( naturalIdAnn != null ) {
			if ( joinColumns != null ) {
				for ( Ejb3Column column : joinColumns ) {
					String keyName = "UK_" + Constraint.hashedName( column.getTable().getName() + "_NaturalID" );
					column.addUniqueKey( keyName, inSecondPass );
				}
			}
			else {
				for ( Ejb3Column column : columns ) {
					String keyName = "UK_" + Constraint.hashedName( column.getTable().getName() + "_NaturalID" );
					column.addUniqueKey( keyName, inSecondPass );
				}
			}
		}
	}

	private static boolean isToManyAssociationWithinEmbeddableCollection(PropertyHolder propertyHolder) {
		if(propertyHolder instanceof ComponentPropertyHolder) {
			ComponentPropertyHolder componentPropertyHolder = (ComponentPropertyHolder) propertyHolder;
			return componentPropertyHolder.isWithinElementCollection();
		}
		return false;
	}

	private static void setVersionInformation(XProperty property, PropertyBinder propertyBinder) {
		propertyBinder.getSimpleValueBinder().setVersion( true );
		if(property.isAnnotationPresent( Source.class )) {
			Source source = property.getAnnotation( Source.class );
			propertyBinder.getSimpleValueBinder().setTimestampVersionType( source.value().typeName() );
		}
	}

	private static void processId(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			SimpleValue idValue,
			HashMap<String, IdentifierGeneratorDefinition> classGenerators,
			boolean isIdentifierMapper,
			MetadataBuildingContext buildingContext) {
		if ( isIdentifierMapper ) {
			throw new AnnotationException(
					"@IdClass class should not have @Id nor @EmbeddedId properties: "
							+ BinderHelper.getPath( propertyHolder, inferredData )
			);
		}
		XClass returnedClass = inferredData.getClassOrElement();
		XProperty property = inferredData.getProperty();
		//clone classGenerator and override with local values
		HashMap<String, IdentifierGeneratorDefinition> localGenerators = ( HashMap<String, IdentifierGeneratorDefinition> ) classGenerators.clone();
		localGenerators.putAll( buildLocalGenerators( property, buildingContext ) );

		//manage composite related metadata
		//guess if its a component and find id data access (property, field etc)
		final boolean isComponent = returnedClass.isAnnotationPresent( Embeddable.class )
				|| property.isAnnotationPresent( EmbeddedId.class );

		GeneratedValue generatedValue = property.getAnnotation( GeneratedValue.class );
		String generatorType = generatedValue != null
				? generatorType( generatedValue.strategy(), buildingContext, returnedClass )
				: "assigned";
		String generatorName = generatedValue != null
				? generatedValue.generator()
				: BinderHelper.ANNOTATION_STRING_DEFAULT;
		if ( isComponent ) {
			//a component must not have any generator
			generatorType = "assigned";
		}
		BinderHelper.makeIdGenerator( idValue, generatorType, generatorName, buildingContext, localGenerators );

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Bind {0} on {1}", ( isComponent ? "@EmbeddedId" : "@Id" ), inferredData.getPropertyName() );
		}
	}

	//TODO move that to collection binder?

	private static void bindJoinedTableAssociation(
			XProperty property,
			MetadataBuildingContext buildingContext,
			EntityBinder entityBinder,
			CollectionBinder collectionBinder,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String mappedBy) {
		TableBinder associationTableBinder = new TableBinder();
		JoinColumn[] annJoins;
		JoinColumn[] annInverseJoins;
		JoinTable assocTable = propertyHolder.getJoinTable( property );
		CollectionTable collectionTable = property.getAnnotation( CollectionTable.class );
		if ( assocTable != null || collectionTable != null ) {

			final String catalog;
			final String schema;
			final String tableName;
			final UniqueConstraint[] uniqueConstraints;
			final JoinColumn[] joins;
			final JoinColumn[] inverseJoins;
			final javax.persistence.Index[] jpaIndexes;


			//JPA 2 has priority
			if ( collectionTable != null ) {
				catalog = collectionTable.catalog();
				schema = collectionTable.schema();
				tableName = collectionTable.name();
				uniqueConstraints = collectionTable.uniqueConstraints();
				joins = collectionTable.joinColumns();
				inverseJoins = null;
				jpaIndexes = collectionTable.indexes();
			}
			else {
				catalog = assocTable.catalog();
				schema = assocTable.schema();
				tableName = assocTable.name();
				uniqueConstraints = assocTable.uniqueConstraints();
				joins = assocTable.joinColumns();
				inverseJoins = assocTable.inverseJoinColumns();
				jpaIndexes = assocTable.indexes();
			}

			collectionBinder.setExplicitAssociationTable( true );
			if ( jpaIndexes != null && jpaIndexes.length > 0 ) {
				associationTableBinder.setJpaIndex( jpaIndexes );
			}
			if ( !BinderHelper.isEmptyAnnotationValue( schema ) ) {
				associationTableBinder.setSchema( schema );
			}
			if ( !BinderHelper.isEmptyAnnotationValue( catalog ) ) {
				associationTableBinder.setCatalog( catalog );
			}
			if ( !BinderHelper.isEmptyAnnotationValue( tableName ) ) {
				associationTableBinder.setName( tableName );
			}
			associationTableBinder.setUniqueConstraints( uniqueConstraints );
			associationTableBinder.setJpaIndex( jpaIndexes );
			//set check constaint in the second pass
			annJoins = joins.length == 0 ? null : joins;
			annInverseJoins = inverseJoins == null || inverseJoins.length == 0 ? null : inverseJoins;
		}
		else {
			annJoins = null;
			annInverseJoins = null;
		}
		Ejb3JoinColumn[] joinColumns = Ejb3JoinColumn.buildJoinTableJoinColumns(
				annJoins,
				entityBinder.getSecondaryTables(),
				propertyHolder,
				inferredData.getPropertyName(),
				mappedBy,
				buildingContext
		);
		Ejb3JoinColumn[] inverseJoinColumns = Ejb3JoinColumn.buildJoinTableJoinColumns(
				annInverseJoins,
				entityBinder.getSecondaryTables(),
				propertyHolder,
				inferredData.getPropertyName(),
				mappedBy,
				buildingContext
		);
		associationTableBinder.setBuildingContext( buildingContext );
		collectionBinder.setTableBinder( associationTableBinder );
		collectionBinder.setJoinColumns( joinColumns );
		collectionBinder.setInverseJoinColumns( inverseJoinColumns );
	}

	private static PropertyBinder bindComponent(
			PropertyData inferredData,
			PropertyHolder propertyHolder,
			AccessType propertyAccessor,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext buildingContext,
			boolean isComponentEmbedded,
			boolean isId, //is a identifier
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			String referencedEntityName, //is a component who is overridden by a @MapsId
			Ejb3JoinColumn[] columns) {
		Component comp;
		if ( referencedEntityName != null ) {
			comp = createComponent( propertyHolder, inferredData, isComponentEmbedded, isIdentifierMapper, buildingContext );
			SecondPass sp = new CopyIdentifierComponentSecondPass(
					comp,
					referencedEntityName,
					columns,
					buildingContext
			);
			buildingContext.getMetadataCollector().addSecondPass( sp );
		}
		else {
			comp = fillComponent(
					propertyHolder, inferredData, propertyAccessor, !isId, entityBinder,
					isComponentEmbedded, isIdentifierMapper,
					false, buildingContext, inheritanceStatePerClass
			);
		}
		if ( isId ) {
			comp.setKey( true );
			if ( propertyHolder.getPersistentClass().getIdentifier() != null ) {
				throw new AnnotationException(
						comp.getComponentClassName()
								+ " must not have @Id properties when used as an @EmbeddedId: "
								+ BinderHelper.getPath( propertyHolder, inferredData )
				);
			}
			if ( referencedEntityName == null && comp.getPropertySpan() == 0 ) {
				throw new AnnotationException(
						comp.getComponentClassName()
								+ " has no persistent id property: "
								+ BinderHelper.getPath( propertyHolder, inferredData )
				);
			}
		}
		XProperty property = inferredData.getProperty();
		setupComponentTuplizer( property, comp );
		PropertyBinder binder = new PropertyBinder();
		binder.setDeclaringClass(inferredData.getDeclaringClass());
		binder.setName( inferredData.getPropertyName() );
		binder.setValue( comp );
		binder.setProperty( inferredData.getProperty() );
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setEmbedded( isComponentEmbedded );
		binder.setHolder( propertyHolder );
		binder.setId( isId );
		binder.setEntityBinder( entityBinder );
		binder.setInheritanceStatePerClass( inheritanceStatePerClass );
		binder.setBuildingContext( buildingContext );
		binder.makePropertyAndBind();
		return binder;
	}

	public static Component fillComponent(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			AccessType propertyAccessor,
			boolean isNullable,
			EntityBinder entityBinder,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			MetadataBuildingContext buildingContext,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		return fillComponent(
				propertyHolder,
				inferredData,
				null,
				propertyAccessor,
				isNullable,
				entityBinder,
				isComponentEmbedded,
				isIdentifierMapper,
				inSecondPass,
				buildingContext,
				inheritanceStatePerClass
		);
	}

	public static Component fillComponent(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			PropertyData baseInferredData, //base inferred data correspond to the entity reproducing inferredData's properties (ie IdClass)
			AccessType propertyAccessor,
			boolean isNullable,
			EntityBinder entityBinder,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			MetadataBuildingContext buildingContext,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		/**
		 * inSecondPass can only be used to apply right away the second pass of a composite-element
		 * Because it's a value type, there is no bidirectional association, hence second pass
		 * ordering does not matter
		 */
		Component comp = createComponent( propertyHolder, inferredData, isComponentEmbedded, isIdentifierMapper, buildingContext );
		String subpath = BinderHelper.getPath( propertyHolder, inferredData );
		LOG.tracev( "Binding component with path: {0}", subpath );
		PropertyHolder subHolder = PropertyHolderBuilder.buildPropertyHolder(
				comp,
				subpath,
				inferredData,
				propertyHolder,
				buildingContext
		);


		// propertyHolder here is the owner of the component property.  Tell it we are about to start the component...

		propertyHolder.startingProperty( inferredData.getProperty() );

		final XClass xClassProcessed = inferredData.getPropertyClass();
		List<PropertyData> classElements = new ArrayList<PropertyData>();
		XClass returnedClassOrElement = inferredData.getClassOrElement();

		List<PropertyData> baseClassElements = null;
		Map<String, PropertyData> orderedBaseClassElements = new HashMap<String, PropertyData>();
		XClass baseReturnedClassOrElement;
		if ( baseInferredData != null ) {
			baseClassElements = new ArrayList<PropertyData>();
			baseReturnedClassOrElement = baseInferredData.getClassOrElement();
			bindTypeDefs( baseReturnedClassOrElement, buildingContext );
			PropertyContainer propContainer = new PropertyContainer( baseReturnedClassOrElement, xClassProcessed, propertyAccessor );
			addElementsOfClass( baseClassElements,  propContainer, buildingContext );
			for ( PropertyData element : baseClassElements ) {
				orderedBaseClassElements.put( element.getPropertyName(), element );
			}
		}

		//embeddable elements can have type defs
		bindTypeDefs( returnedClassOrElement, buildingContext );
		PropertyContainer propContainer = new PropertyContainer( returnedClassOrElement, xClassProcessed, propertyAccessor );
		addElementsOfClass( classElements, propContainer, buildingContext );

		//add elements of the embeddable superclass
		XClass superClass = xClassProcessed.getSuperclass();
		while ( superClass != null && superClass.isAnnotationPresent( MappedSuperclass.class ) ) {
			//FIXME: proper support of typevariables incl var resolved at upper levels
			propContainer = new PropertyContainer( superClass, xClassProcessed, propertyAccessor );
			addElementsOfClass( classElements, propContainer, buildingContext );
			superClass = superClass.getSuperclass();
		}
		if ( baseClassElements != null ) {
			//useful to avoid breaking pre JPA 2 mappings
			if ( !hasAnnotationsOnIdClass( xClassProcessed ) ) {
				for ( int i = 0; i < classElements.size(); i++ ) {
					final PropertyData idClassPropertyData = classElements.get( i );
					final PropertyData entityPropertyData = orderedBaseClassElements.get( idClassPropertyData.getPropertyName() );
					if ( propertyHolder.isInIdClass() ) {
						if ( entityPropertyData == null ) {
							throw new AnnotationException(
									"Property of @IdClass not found in entity "
											+ baseInferredData.getPropertyClass().getName() + ": "
											+ idClassPropertyData.getPropertyName()
							);
						}
						final boolean hasXToOneAnnotation = entityPropertyData.getProperty()
								.isAnnotationPresent( ManyToOne.class )
								|| entityPropertyData.getProperty().isAnnotationPresent( OneToOne.class );
						final boolean isOfDifferentType = !entityPropertyData.getClassOrElement()
								.equals( idClassPropertyData.getClassOrElement() );
						if ( hasXToOneAnnotation && isOfDifferentType ) {
							//don't replace here as we need to use the actual original return type
							//the annotation overriding will be dealt with by a mechanism similar to @MapsId
						}
						else {
							classElements.set( i, entityPropertyData );  //this works since they are in the same order
						}
					}
					else {
						classElements.set( i, entityPropertyData );  //this works since they are in the same order
					}
				}
			}
		}
		for ( PropertyData propertyAnnotatedElement : classElements ) {
			processElementAnnotations(
					subHolder,
					isNullable
							? Nullability.NO_CONSTRAINT
							: Nullability.FORCED_NOT_NULL,
					propertyAnnotatedElement,
					new HashMap<String, IdentifierGeneratorDefinition>(),
					entityBinder,
					isIdentifierMapper,
					isComponentEmbedded,
					inSecondPass,
					buildingContext,
					inheritanceStatePerClass
			);

			XProperty property = propertyAnnotatedElement.getProperty();
			if ( property.isAnnotationPresent( GeneratedValue.class ) &&
					property.isAnnotationPresent( Id.class ) ) {
				//clone classGenerator and override with local values
				Map<String, IdentifierGeneratorDefinition> localGenerators = new HashMap<String, IdentifierGeneratorDefinition>();
				localGenerators.putAll( buildLocalGenerators( property, buildingContext ) );

				GeneratedValue generatedValue = property.getAnnotation( GeneratedValue.class );
				String generatorType = generatedValue != null
						? generatorType( generatedValue.strategy(), buildingContext, property.getType() )
						: "assigned";
				String generator = generatedValue != null ? generatedValue.generator() : BinderHelper.ANNOTATION_STRING_DEFAULT;

				BinderHelper.makeIdGenerator(
						( SimpleValue ) comp.getProperty( property.getName() ).getValue(),
						generatorType,
						generator,
						buildingContext,
						localGenerators
				);
			}

		}
		return comp;
	}

	public static Component createComponent(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			MetadataBuildingContext context) {
		Component comp = new Component( context.getMetadataCollector(), propertyHolder.getPersistentClass() );
		comp.setEmbedded( isComponentEmbedded );
		//yuk
		comp.setTable( propertyHolder.getTable() );
		//FIXME shouldn't identifier mapper use getClassOrElementName? Need to be checked.
		if ( isIdentifierMapper || ( isComponentEmbedded && inferredData.getPropertyName() == null ) ) {
			comp.setComponentClassName( comp.getOwner().getClassName() );
		}
		else {
			comp.setComponentClassName( inferredData.getClassOrElementName() );
		}
		return comp;
	}

	private static void bindIdClass(
			String generatorType,
			String generatorName,
			PropertyData inferredData,
			PropertyData baseInferredData,
			Ejb3Column[] columns,
			PropertyHolder propertyHolder,
			boolean isComposite,
			AccessType propertyAccessor,
			EntityBinder entityBinder,
			boolean isEmbedded,
			boolean isIdentifierMapper,
			MetadataBuildingContext buildingContext,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {

		/*
		 * Fill simple value and property since and Id is a property
		 */
		PersistentClass persistentClass = propertyHolder.getPersistentClass();
		if ( !( persistentClass instanceof RootClass ) ) {
			throw new AnnotationException(
					"Unable to define/override @Id(s) on a subclass: "
							+ propertyHolder.getEntityName()
			);
		}
		RootClass rootClass = ( RootClass ) persistentClass;
		String persistentClassName = rootClass.getClassName();
		SimpleValue id;
		final String propertyName = inferredData.getPropertyName();
		HashMap<String, IdGenerator> localGenerators = new HashMap<String, IdGenerator>();
		if ( isComposite ) {
			id = fillComponent(
					propertyHolder,
					inferredData,
					baseInferredData,
					propertyAccessor,
					false,
					entityBinder,
					isEmbedded,
					isIdentifierMapper,
					false,
					buildingContext,
					inheritanceStatePerClass
			);
			Component componentId = ( Component ) id;
			componentId.setKey( true );
			if ( rootClass.getIdentifier() != null ) {
				throw new AnnotationException( componentId.getComponentClassName() + " must not have @Id properties when used as an @EmbeddedId" );
			}
			if ( componentId.getPropertySpan() == 0 ) {
				throw new AnnotationException( componentId.getComponentClassName() + " has no persistent id property" );
			}
			//tuplizers
			XProperty property = inferredData.getProperty();
			setupComponentTuplizer( property, componentId );
		}
		else {
			//TODO I think this branch is never used. Remove.

			for ( Ejb3Column column : columns ) {
				column.forceNotNull(); //this is an id
			}
			SimpleValueBinder value = new SimpleValueBinder();
			value.setPropertyName( propertyName );
			value.setReturnedClassName( inferredData.getTypeName() );
			value.setColumns( columns );
			value.setPersistentClassName( persistentClassName );
			value.setBuildingContext( buildingContext );
			value.setType( inferredData.getProperty(), inferredData.getClassOrElement(), persistentClassName, null );
			value.setAccessType( propertyAccessor );
			id = value.make();
		}
		rootClass.setIdentifier( id );
		BinderHelper.makeIdGenerator(
				id,
				generatorType,
				generatorName,
				buildingContext,
				Collections.<String, IdentifierGeneratorDefinition>emptyMap()
		);
		if ( isEmbedded ) {
			rootClass.setEmbeddedIdentifier( inferredData.getPropertyClass() == null );
		}
		else {
			PropertyBinder binder = new PropertyBinder();
			binder.setName( propertyName );
			binder.setValue( id );
			binder.setAccessType( inferredData.getDefaultAccess() );
			binder.setProperty( inferredData.getProperty() );
			Property prop = binder.makeProperty();
			rootClass.setIdentifierProperty( prop );
			//if the id property is on a superclass, update the metamodel
			final org.hibernate.mapping.MappedSuperclass superclass = BinderHelper.getMappedSuperclassOrNull(
					inferredData.getDeclaringClass(),
					inheritanceStatePerClass,
					buildingContext
			);
			if ( superclass != null ) {
				superclass.setDeclaredIdentifierProperty( prop );
			}
			else {
				//we know the property is on the actual entity
				rootClass.setDeclaredIdentifierProperty( prop );
			}
		}
	}

	private static PropertyData getUniqueIdPropertyFromBaseClass(
			PropertyData inferredData,
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			MetadataBuildingContext context) {
		List<PropertyData> baseClassElements = new ArrayList<PropertyData>();
		XClass baseReturnedClassOrElement = baseInferredData.getClassOrElement();
		PropertyContainer propContainer = new PropertyContainer(
				baseReturnedClassOrElement,
				inferredData.getPropertyClass(),
				propertyAccessor
		);
		addElementsOfClass( baseClassElements, propContainer, context );
		//Id properties are on top and there is only one
		return baseClassElements.get( 0 );
	}

	private static void setupComponentTuplizer(XProperty property, Component component) {
		if ( property == null ) {
			return;
		}
		if ( property.isAnnotationPresent( Tuplizers.class ) ) {
			for ( Tuplizer tuplizer : property.getAnnotation( Tuplizers.class ).value() ) {
				EntityMode mode = EntityMode.parse( tuplizer.entityMode() );
				//todo tuplizer.entityModeType
				component.addTuplizer( mode, tuplizer.impl().getName() );
			}
		}
		if ( property.isAnnotationPresent( Tuplizer.class ) ) {
			Tuplizer tuplizer = property.getAnnotation( Tuplizer.class );
			EntityMode mode = EntityMode.parse( tuplizer.entityMode() );
			//todo tuplizer.entityModeType
			component.addTuplizer( mode, tuplizer.impl().getName() );
		}
	}

	private static void bindManyToOne(
			String cascadeStrategy,
			Ejb3JoinColumn[] columns,
			boolean optional,
			boolean ignoreNotFound,
			boolean cascadeOnDelete,
			XClass targetEntity,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean unique,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			PropertyBinder propertyBinder,
			MetadataBuildingContext context) {
		//All FK columns should be in the same table
		org.hibernate.mapping.ManyToOne value = new org.hibernate.mapping.ManyToOne( context.getMetadataCollector(), columns[0].getTable() );
		// This is a @OneToOne mapped to a physical o.h.mapping.ManyToOne
		if ( unique ) {
			value.markAsLogicalOneToOne();
		}
		value.setReferencedEntityName( ToOneBinder.getReferenceEntityName( inferredData, targetEntity, context ) );
		final XProperty property = inferredData.getProperty();
		defineFetchingStrategy( value, property );
		//value.setFetchMode( fetchMode );
		value.setIgnoreNotFound( ignoreNotFound );
		value.setCascadeDeleteEnabled( cascadeOnDelete );
		//value.setLazy( fetchMode != FetchMode.JOIN );
		if ( !optional ) {
			for ( Ejb3JoinColumn column : columns ) {
				column.setNullable( false );
			}
		}
		if ( property.isAnnotationPresent( MapsId.class ) ) {
			//read only
			for ( Ejb3JoinColumn column : columns ) {
				column.setInsertable( false );
				column.setUpdatable( false );
			}
		}
		
		final JoinColumn joinColumn = property.getAnnotation( JoinColumn.class );

		//Make sure that JPA1 key-many-to-one columns are read only tooj
		boolean hasSpecjManyToOne=false;
		if ( context.getBuildingOptions().isSpecjProprietarySyntaxEnabled() ) {
			String columnName = "";
			for ( XProperty prop : inferredData.getDeclaringClass()
					.getDeclaredProperties( AccessType.FIELD.getType() ) ) {
				if ( prop.isAnnotationPresent( Id.class ) && prop.isAnnotationPresent( Column.class ) ) {
					columnName = prop.getAnnotation( Column.class ).name();
				}

				if ( property.isAnnotationPresent( ManyToOne.class ) && joinColumn != null
						&& ! BinderHelper.isEmptyAnnotationValue( joinColumn.name() )
						&& joinColumn.name().equals( columnName )
						&& !property.isAnnotationPresent( MapsId.class ) ) {
					hasSpecjManyToOne = true;
					for ( Ejb3JoinColumn column : columns ) {
						column.setInsertable( false );
						column.setUpdatable( false );
					}
				}
			}

		}
		value.setTypeName( inferredData.getClassOrElementName() );
		final String propertyName = inferredData.getPropertyName();
		value.setTypeUsingReflection( propertyHolder.getClassName(), propertyName );

		if ( joinColumn != null
				&& joinColumn.foreignKey().value() == ConstraintMode.NO_CONSTRAINT ) {
			// not ideal...
			value.setForeignKeyName( "none" );
		}
		else {
			final ForeignKey fk = property.getAnnotation( ForeignKey.class );
			if ( fk != null && StringHelper.isNotEmpty( fk.name() ) ) {
				value.setForeignKeyName( fk.name() );
			}
			else if ( joinColumn != null ) {
				value.setForeignKeyName( StringHelper.nullIfEmpty( joinColumn.foreignKey().name() ) );
				value.setForeignKeyDefinition( StringHelper.nullIfEmpty( joinColumn.foreignKey().foreignKeyDefinition() ) );
			}
		}

		String path = propertyHolder.getPath() + "." + propertyName;
		FkSecondPass secondPass = new ToOneFkSecondPass(
				value, columns,
				!optional && unique, //cannot have nullable and unique on certain DBs like Derby
				propertyHolder.getEntityOwnerClassName(),
				path,
				context
		);
		if ( inSecondPass ) {
			secondPass.doSecondPass( context.getMetadataCollector().getEntityBindingMap() );
		}
		else {
			context.getMetadataCollector().addSecondPass( secondPass );
		}
		Ejb3Column.checkPropertyConsistency( columns, propertyHolder.getEntityName() + "." + propertyName );
		//PropertyBinder binder = new PropertyBinder();
		propertyBinder.setName( propertyName );
		propertyBinder.setValue( value );
		//binder.setCascade(cascadeStrategy);
		if ( isIdentifierMapper ) {
			propertyBinder.setInsertable( false );
			propertyBinder.setUpdatable( false );
		}
		else if (hasSpecjManyToOne) {
			propertyBinder.setInsertable( false );
			propertyBinder.setUpdatable( false );
		}
		else {
			propertyBinder.setInsertable( columns[0].isInsertable() );
			propertyBinder.setUpdatable( columns[0].isUpdatable() );
		}
		propertyBinder.setColumns( columns );
		propertyBinder.setAccessType( inferredData.getDefaultAccess() );
		propertyBinder.setCascade( cascadeStrategy );
		propertyBinder.setProperty( property );
		propertyBinder.setXToMany( true );
		propertyBinder.makePropertyAndBind();
	}

	protected static void defineFetchingStrategy(ToOne toOne, XProperty property) {
		LazyToOne lazy = property.getAnnotation( LazyToOne.class );
		Fetch fetch = property.getAnnotation( Fetch.class );
		ManyToOne manyToOne = property.getAnnotation( ManyToOne.class );
		OneToOne oneToOne = property.getAnnotation( OneToOne.class );
		FetchType fetchType;
		if ( manyToOne != null ) {
			fetchType = manyToOne.fetch();
		}
		else if ( oneToOne != null ) {
			fetchType = oneToOne.fetch();
		}
		else {
			throw new AssertionFailure(
					"Define fetch strategy on a property not annotated with @OneToMany nor @OneToOne"
			);
		}
		if ( lazy != null ) {
			toOne.setLazy( !( lazy.value() == LazyToOneOption.FALSE ) );
			toOne.setUnwrapProxy( ( lazy.value() == LazyToOneOption.NO_PROXY ) );
		}
		else {
			toOne.setLazy( fetchType == FetchType.LAZY );
			toOne.setUnwrapProxy( false );
		}
		if ( fetch != null ) {
			if ( fetch.value() == org.hibernate.annotations.FetchMode.JOIN ) {
				toOne.setFetchMode( FetchMode.JOIN );
				toOne.setLazy( false );
				toOne.setUnwrapProxy( false );
			}
			else if ( fetch.value() == org.hibernate.annotations.FetchMode.SELECT ) {
				toOne.setFetchMode( FetchMode.SELECT );
			}
			else if ( fetch.value() == org.hibernate.annotations.FetchMode.SUBSELECT ) {
				throw new AnnotationException( "Use of FetchMode.SUBSELECT not allowed on ToOne associations" );
			}
			else {
				throw new AssertionFailure( "Unknown FetchMode: " + fetch.value() );
			}
		}
		else {
			toOne.setFetchMode( getFetchMode( fetchType ) );
		}
	}

	private static void bindOneToOne(
			String cascadeStrategy,
			Ejb3JoinColumn[] joinColumns,
			boolean optional,
			FetchMode fetchMode,
			boolean ignoreNotFound,
			boolean cascadeOnDelete,
			XClass targetEntity,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String mappedBy,
			boolean trueOneToOne,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			PropertyBinder propertyBinder,
			MetadataBuildingContext context) {
		//column.getTable() => persistentClass.getTable()
		final String propertyName = inferredData.getPropertyName();
		LOG.tracev( "Fetching {0} with {1}", propertyName, fetchMode );
		boolean mapToPK = true;
		if ( !trueOneToOne ) {
			//try to find a hidden true one to one (FK == PK columns)
			KeyValue identifier = propertyHolder.getIdentifier();
			if ( identifier == null ) {
				//this is a @OneToOne in a @EmbeddedId (the persistentClass.identifier is not set yet, it's being built)
				//by definition the PK cannot refers to itself so it cannot map to itself
				mapToPK = false;
			}
			else {
				Iterator idColumns = identifier.getColumnIterator();
				List<String> idColumnNames = new ArrayList<String>();
				org.hibernate.mapping.Column currentColumn;
				if ( identifier.getColumnSpan() != joinColumns.length ) {
					mapToPK = false;
				}
				else {
					while ( idColumns.hasNext() ) {
						currentColumn = ( org.hibernate.mapping.Column ) idColumns.next();
						idColumnNames.add( currentColumn.getName() );
					}
					for ( Ejb3JoinColumn col : joinColumns ) {
						if ( !idColumnNames.contains( col.getMappingColumn().getName() ) ) {
							mapToPK = false;
							break;
						}
					}
				}
			}
		}
		if ( trueOneToOne || mapToPK || !BinderHelper.isEmptyAnnotationValue( mappedBy ) ) {
			//is a true one-to-one
			//FIXME referencedColumnName ignored => ordering may fail.
			OneToOneSecondPass secondPass = new OneToOneSecondPass(
					mappedBy,
					propertyHolder.getEntityName(),
					propertyName,
					propertyHolder,
					inferredData,
					targetEntity,
					ignoreNotFound,
					cascadeOnDelete,
					optional,
					cascadeStrategy,
					joinColumns,
					context
			);
			if ( inSecondPass ) {
				secondPass.doSecondPass( context.getMetadataCollector().getEntityBindingMap() );
			}
			else {
				context.getMetadataCollector().addSecondPass(
						secondPass,
						BinderHelper.isEmptyAnnotationValue( mappedBy )
				);
			}
		}
		else {
			//has a FK on the table
			bindManyToOne(
					cascadeStrategy, joinColumns, optional, ignoreNotFound, cascadeOnDelete,
					targetEntity,
					propertyHolder, inferredData, true, isIdentifierMapper, inSecondPass,
					propertyBinder, context
			);
		}
	}

	private static void bindAny(
			String cascadeStrategy,
			Ejb3JoinColumn[] columns,
			boolean cascadeOnDelete,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext buildingContext) {
		org.hibernate.annotations.Any anyAnn = inferredData.getProperty()
				.getAnnotation( org.hibernate.annotations.Any.class );
		if ( anyAnn == null ) {
			throw new AssertionFailure(
					"Missing @Any annotation: "
							+ BinderHelper.getPath( propertyHolder, inferredData )
			);
		}
		Any value = BinderHelper.buildAnyValue(
				anyAnn.metaDef(),
				columns,
				anyAnn.metaColumn(),
				inferredData,
				cascadeOnDelete,
				nullability,
				propertyHolder,
				entityBinder,
				anyAnn.optional(),
				buildingContext
		);

		PropertyBinder binder = new PropertyBinder();
		binder.setName( inferredData.getPropertyName() );
		binder.setValue( value );

		binder.setLazy( anyAnn.fetch() == FetchType.LAZY );
		//binder.setCascade(cascadeStrategy);
		if ( isIdentifierMapper ) {
			binder.setInsertable( false );
			binder.setUpdatable( false );
		}
		else {
			binder.setInsertable( columns[0].isInsertable() );
			binder.setUpdatable( columns[0].isUpdatable() );
		}
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setCascade( cascadeStrategy );
		Property prop = binder.makeProperty();
		//composite FK columns are in the same table so its OK
		propertyHolder.addProperty( prop, columns, inferredData.getDeclaringClass() );
	}

	private static String generatorType(
			GenerationType generatorEnum,
			final MetadataBuildingContext buildingContext,
			final XClass javaTypeXClass) {
		return buildingContext.getBuildingOptions().getIdGenerationTypeInterpreter().determineGeneratorName(
				generatorEnum,
				new IdGeneratorStrategyInterpreter.GeneratorNameDeterminationContext() {
					Class javaType = null;
					@Override
					public Class getIdType() {
						if ( javaType == null ) {
							javaType = buildingContext.getBuildingOptions()
									.getReflectionManager()
									.toClass( javaTypeXClass );
						}
						return javaType;
					}
				}
		);
	}

	private static EnumSet<CascadeType> convertToHibernateCascadeType(javax.persistence.CascadeType[] ejbCascades) {
		EnumSet<CascadeType> hibernateCascadeSet = EnumSet.noneOf( CascadeType.class );
		if ( ejbCascades != null && ejbCascades.length > 0 ) {
			for ( javax.persistence.CascadeType cascade : ejbCascades ) {
				switch ( cascade ) {
					case ALL:
						hibernateCascadeSet.add( CascadeType.ALL );
						break;
					case PERSIST:
						hibernateCascadeSet.add( CascadeType.PERSIST );
						break;
					case MERGE:
						hibernateCascadeSet.add( CascadeType.MERGE );
						break;
					case REMOVE:
						hibernateCascadeSet.add( CascadeType.REMOVE );
						break;
					case REFRESH:
						hibernateCascadeSet.add( CascadeType.REFRESH );
						break;
					case DETACH:
						hibernateCascadeSet.add( CascadeType.DETACH );
						break;
				}
			}
		}

		return hibernateCascadeSet;
	}

	private static String getCascadeStrategy(
			javax.persistence.CascadeType[] ejbCascades,
			Cascade hibernateCascadeAnnotation,
			boolean orphanRemoval,
			boolean forcePersist) {
		EnumSet<CascadeType> hibernateCascadeSet = convertToHibernateCascadeType( ejbCascades );
		CascadeType[] hibernateCascades = hibernateCascadeAnnotation == null ?
				null :
				hibernateCascadeAnnotation.value();

		if ( hibernateCascades != null && hibernateCascades.length > 0 ) {
			hibernateCascadeSet.addAll( Arrays.asList( hibernateCascades ) );
		}

		if ( orphanRemoval ) {
			hibernateCascadeSet.add( CascadeType.DELETE_ORPHAN );
			hibernateCascadeSet.add( CascadeType.REMOVE );
		}
		if ( forcePersist ) {
			hibernateCascadeSet.add( CascadeType.PERSIST );
		}

		StringBuilder cascade = new StringBuilder();
		for ( CascadeType aHibernateCascadeSet : hibernateCascadeSet ) {
			switch ( aHibernateCascadeSet ) {
				case ALL:
					cascade.append( "," ).append( "all" );
					break;
				case SAVE_UPDATE:
					cascade.append( "," ).append( "save-update" );
					break;
				case PERSIST:
					cascade.append( "," ).append( "persist" );
					break;
				case MERGE:
					cascade.append( "," ).append( "merge" );
					break;
				case LOCK:
					cascade.append( "," ).append( "lock" );
					break;
				case REFRESH:
					cascade.append( "," ).append( "refresh" );
					break;
				case REPLICATE:
					cascade.append( "," ).append( "replicate" );
					break;
				case EVICT:
				case DETACH:
					cascade.append( "," ).append( "evict" );
					break;
				case DELETE:
					cascade.append( "," ).append( "delete" );
					break;
				case DELETE_ORPHAN:
					cascade.append( "," ).append( "delete-orphan" );
					break;
				case REMOVE:
					cascade.append( "," ).append( "delete" );
					break;
			}
		}
		return cascade.length() > 0 ?
				cascade.substring( 1 ) :
				"none";
	}

	public static FetchMode getFetchMode(FetchType fetch) {
		if ( fetch == FetchType.EAGER ) {
			return FetchMode.JOIN;
		}
		else {
			return FetchMode.SELECT;
		}
	}

	private static HashMap<String, IdentifierGeneratorDefinition> buildLocalGenerators(XAnnotatedElement annElt, MetadataBuildingContext context) {
		HashMap<String, IdentifierGeneratorDefinition> generators = new HashMap<String, IdentifierGeneratorDefinition>();
		TableGenerator tabGen = annElt.getAnnotation( TableGenerator.class );
		SequenceGenerator seqGen = annElt.getAnnotation( SequenceGenerator.class );
		GenericGenerator genGen = annElt.getAnnotation( GenericGenerator.class );
		if ( tabGen != null ) {
			IdentifierGeneratorDefinition idGen = buildIdGenerator( tabGen, context );
			generators.put( idGen.getName(), idGen );
		}
		if ( seqGen != null ) {
			IdentifierGeneratorDefinition idGen = buildIdGenerator( seqGen, context );
			generators.put( idGen.getName(), idGen );
		}
		if ( genGen != null ) {
			IdentifierGeneratorDefinition idGen = buildIdGenerator( genGen, context );
			generators.put( idGen.getName(), idGen );
		}
		return generators;
	}

	public static boolean isDefault(XClass clazz, MetadataBuildingContext context) {
		return context.getBuildingOptions().getReflectionManager().equals( clazz, void.class );
	}

	/**
	 * For the mapped entities build some temporary data-structure containing information about the
	 * inheritance status of a class.
	 *
	 * @param orderedClasses Order list of all annotated entities and their mapped superclasses
	 *
	 * @return A map of {@code InheritanceState}s keyed against their {@code XClass}.
	 */
	public static Map<XClass, InheritanceState> buildInheritanceStates(
			List<XClass> orderedClasses,
			MetadataBuildingContext buildingContext) {
		ReflectionManager reflectionManager = buildingContext.getBuildingOptions().getReflectionManager();
		Map<XClass, InheritanceState> inheritanceStatePerClass = new HashMap<XClass, InheritanceState>(
				orderedClasses.size()
		);
		for ( XClass clazz : orderedClasses ) {
			InheritanceState superclassState = InheritanceState.getSuperclassInheritanceState(
					clazz, inheritanceStatePerClass
			);
			InheritanceState state = new InheritanceState( clazz, inheritanceStatePerClass, buildingContext );
			if ( superclassState != null ) {
				//the classes are ordered thus preventing an NPE
				//FIXME if an entity has subclasses annotated @MappedSperclass wo sub @Entity this is wrong
				superclassState.setHasSiblings( true );
				InheritanceState superEntityState = InheritanceState.getInheritanceStateOfSuperEntity(
						clazz, inheritanceStatePerClass
				);
				state.setHasParents( superEntityState != null );
				final boolean nonDefault = state.getType() != null && !InheritanceType.SINGLE_TABLE
						.equals( state.getType() );
				if ( superclassState.getType() != null ) {
					final boolean mixingStrategy = state.getType() != null && !state.getType()
							.equals( superclassState.getType() );
					if ( nonDefault && mixingStrategy ) {
						LOG.invalidSubStrategy( clazz.getName() );
					}
					state.setType( superclassState.getType() );
				}
			}
			inheritanceStatePerClass.put( clazz, state );
		}
		return inheritanceStatePerClass;
	}

	private static boolean hasAnnotationsOnIdClass(XClass idClass) {
//		if(idClass.getAnnotation(Embeddable.class) != null)
//			return true;

		List<XProperty> properties = idClass.getDeclaredProperties( XClass.ACCESS_FIELD );
		for ( XProperty property : properties ) {
			if ( property.isAnnotationPresent( Column.class ) || property.isAnnotationPresent( OneToMany.class ) ||
					property.isAnnotationPresent( ManyToOne.class ) || property.isAnnotationPresent( Id.class ) ||
					property.isAnnotationPresent( GeneratedValue.class ) || property.isAnnotationPresent( OneToOne.class ) ||
					property.isAnnotationPresent( ManyToMany.class )
					) {
				return true;
			}
		}
		List<XMethod> methods = idClass.getDeclaredMethods();
		for ( XMethod method : methods ) {
			if ( method.isAnnotationPresent( Column.class ) || method.isAnnotationPresent( OneToMany.class ) ||
					method.isAnnotationPresent( ManyToOne.class ) || method.isAnnotationPresent( Id.class ) ||
					method.isAnnotationPresent( GeneratedValue.class ) || method.isAnnotationPresent( OneToOne.class ) ||
					method.isAnnotationPresent( ManyToMany.class )
					) {
				return true;
			}
		}
		return false;
	}
}
