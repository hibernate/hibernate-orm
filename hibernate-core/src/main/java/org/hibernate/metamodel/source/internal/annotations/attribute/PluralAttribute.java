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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;

import org.hibernate.TruthValue;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.FetchStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.internal.binder.ForeignKeyDelegate;
import org.hibernate.metamodel.reflite.spi.ArrayDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.CollectionTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.ExplicitTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.source.internal.annotations.util.AssociationHelper;
import org.hibernate.metamodel.source.internal.annotations.util.ConverterAndOverridesHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.PluralAttributeNature;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import static org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames.LOADER;
import static org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames.ON_DELETE;
import static org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames.PERSISTER;
import static org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames.WHERE;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.EMBEDDABLE;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.ENTITY;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.MAP_KEY;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.MAP_KEY_CLASS;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.MAP_KEY_COLUMN;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.MAP_KEY_ENUMERATED;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.MAP_KEY_TEMPORAL;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.ORDER_BY;
import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.ORDER_COLUMN;

/**
 * Represents a plural persistent attribute.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Strong Liu
 */
public class PluralAttribute
		extends AbstractPersistentAttribute
		implements FetchableAttribute, AssociationAttribute {
	private static final EnumSet<PluralAttributeNature> CANNOT_HAVE_COLLECTION_ID = EnumSet.of(
			PluralAttributeNature.SET,
			PluralAttributeNature.MAP,
			PluralAttributeNature.LIST,
			PluralAttributeNature.ARRAY
	);

	private String mappedByAttributeName;
	private final boolean isInverse;
	private final Set<CascadeType> jpaCascadeTypes;
	private final Set<org.hibernate.annotations.CascadeType> hibernateCascadeTypes;
	private final boolean isOrphanRemoval;
	private final boolean ignoreNotFound;
	private final boolean isOptional;
	private final boolean isUnWrapProxy;

	private final FetchStyle fetchStyle;
	private final boolean isLazy;

	// information about the collection
	private final CollectionIdInformation collectionIdInformation;
	private final PluralAttributeNature pluralAttributeNature;
	private final String customPersister;
	private final Caching caching;
	private final String comparatorName;
	private final String customLoaderName;
	private final CustomSQL customInsert;
	private final CustomSQL customUpdate;
	private final CustomSQL customDelete;
	private final CustomSQL customDeleteAll;
	private final String whereClause;
	private final String orderBy;
	private final boolean sorted;
	private final boolean isExtraLazy;
	private final boolean mutable;
	private final int batchSize;

	// information about the FK
	private final OnDeleteAction onDeleteAction;
	private final ForeignKeyDelegate foreignKeyDelegate;

	private final AttributeTypeResolver collectionTypeResolver;

	// information about the element
	private final PluralAttributeElementDetails elementDetails;

	// information about the index
	private final PluralAttributeIndexDetails indexDetails;

	private final AnnotationInstance joinTableAnnotation;
	private ArrayList<Column> joinColumnValues = new ArrayList<Column>();
	private ArrayList<Column> inverseJoinColumnValues = new ArrayList<Column>();
	
	private final ClassLoaderService classLoaderService;

	public PluralAttribute(
			ManagedTypeMetadata container,
			String name,
			AttributePath attributePath,
			AttributeRole attributeRole,
			MemberDescriptor backingMember,
			Nature attributeNature,
			JavaTypeDescriptor elementType,
			JavaTypeDescriptor indexType,
			AccessType accessType,
			String accessorStrategy) {
		super(
				container,
				name,
				attributePath,
				attributeRole,
				backingMember,
				attributeNature,
				accessType,
				accessorStrategy
		);
		
		this.classLoaderService = getContext().getServiceRegistry().getService( ClassLoaderService.class );

		AssociationHelper.INSTANCE.locateMapsId(
				backingMember,
				attributeNature,
				container.getLocalBindingContext()
		);

		final AnnotationInstance associationAnnotation = backingMember.getAnnotations().get( attributeNature.getAnnotationDotName() );
		final AnnotationInstance lazyCollectionAnnotation = backingMember.getAnnotations().get( HibernateDotNames.LAZY_COLLECTION );

		this.mappedByAttributeName = AssociationHelper.INSTANCE.determineMappedByAttributeName( associationAnnotation );

		this.fetchStyle = AssociationHelper.INSTANCE.determineFetchStyle( backingMember );
		this.isLazy = AssociationHelper.INSTANCE.determineWhetherIsLazy(
				associationAnnotation,
				lazyCollectionAnnotation,
				backingMember,
				fetchStyle,
				true
		);
		this.isExtraLazy = determineWhetherIsExtraLazy( lazyCollectionAnnotation );

		this.isOptional = AssociationHelper.INSTANCE.determineOptionality( associationAnnotation );
		this.isUnWrapProxy = AssociationHelper.INSTANCE.determineWhetherToUnwrapProxy( backingMember );

		this.jpaCascadeTypes = AssociationHelper.INSTANCE.determineCascadeTypes( associationAnnotation );
		this.hibernateCascadeTypes = AssociationHelper.INSTANCE.determineHibernateCascadeTypes( backingMember );
		this.isOrphanRemoval = AssociationHelper.INSTANCE.determineOrphanRemoval( associationAnnotation );
		this.ignoreNotFound = AssociationHelper.INSTANCE.determineWhetherToIgnoreNotFound( backingMember );

		this.collectionIdInformation = extractCollectionIdInformation( backingMember );
		this.pluralAttributeNature = resolvePluralAttributeNature( backingMember, collectionIdInformation );

		this.elementDetails = resolveElementDetails( backingMember, attributeNature, elementType );
		this.indexDetails = resolveIndexDetails( backingMember, pluralAttributeNature, indexType );
		this.collectionTypeResolver = new CollectionTypeResolver( this );

		this.mutable = !backingMember.getAnnotations().containsKey( HibernateDotNames.IMMUTABLE );
		final AnnotationInstance batchAnnotation = backingMember.getAnnotations().get( HibernateDotNames.BATCH_SIZE );
		if ( batchAnnotation != null ) {
			this.batchSize = batchAnnotation.value( "size" ).asInt();
		}
		else {
			this.batchSize = -1;
		}

		this.whereClause = determineWereClause( backingMember );
		this.orderBy = determineOrderBy( backingMember );

		this.foreignKeyDelegate = new ForeignKeyDelegate( backingMember.getAnnotations(), classLoaderService );

		this.caching = determineCachingSettings( backingMember );

		this.customPersister = determineCustomPersister( backingMember );
		this.customLoaderName = determineCustomLoaderName( backingMember );
		this.customInsert = AnnotationParserHelper.createCustomSQL(
				backingMember.getAnnotations().get( HibernateDotNames.SQL_INSERT )
		);
		this.customUpdate = AnnotationParserHelper.createCustomSQL(
				backingMember.getAnnotations().get( HibernateDotNames.SQL_UPDATE )
		);
		this.customDelete = AnnotationParserHelper.createCustomSQL(
				backingMember.getAnnotations().get( HibernateDotNames.SQL_DELETE )
		);
		this.customDeleteAll = AnnotationParserHelper.createCustomSQL(
				backingMember.getAnnotations().get( HibernateDotNames.SQL_DELETE_ALL )
		);

		this.onDeleteAction = determineOnDeleteAction( backingMember );

		final AnnotationInstance sortNaturalAnnotation = backingMember.getAnnotations().get( HibernateDotNames.SORT_NATURAL );
		final AnnotationInstance sortComparatorAnnotation = backingMember.getAnnotations().get( HibernateDotNames.SORT_COMPARATOR );
		if ( sortNaturalAnnotation != null ) {
			this.sorted = true;
			this.comparatorName = "natural";
		}
		else if ( sortComparatorAnnotation != null ) {
			this.sorted = true;
			this.comparatorName = sortComparatorAnnotation.value().asString();
		}
		else {
			this.sorted = false;
			this.comparatorName = null;
		}

		if ( this.mappedByAttributeName == null ) {
			// todo : not at all a fan of this mess...
			AssociationHelper.INSTANCE.processJoinColumnAnnotations(
					backingMember,
					joinColumnValues,
					getContext()
			);
			AssociationHelper.INSTANCE.processJoinTableAnnotations(
					backingMember,
					joinColumnValues,
					inverseJoinColumnValues,
					getContext()
			);
			this.joinTableAnnotation = AssociationHelper.INSTANCE.extractExplicitJoinTable(
					backingMember,
					getContext()
			);
			
			final AnnotationInstance inverseAnnotation = backingMember.getAnnotations().get( HibernateDotNames.INVERSE );
			isInverse = inverseAnnotation != null;
			
			// TODO: Temporary!
			if (inverseAnnotation != null && inverseAnnotation.value( "hbmKey" ) != null) {
				final Column joinColumn = new Column();
				joinColumn.setName( inverseAnnotation.value( "hbmKey" ).asString() );
				joinColumnValues.add( joinColumn );
			}
		}
		else {
			this.joinTableAnnotation = null;
			isInverse = true;
		}
		joinColumnValues.trimToSize();
		inverseJoinColumnValues.trimToSize();

		ConverterAndOverridesHelper.INSTANCE.processConverters(
				getPath(),
				getNature(),
				backingMember,
				container,
				getContext()
		);
		ConverterAndOverridesHelper.INSTANCE.processAttributeOverrides(
				getPath(),
				backingMember,
				container,
				getContext()
		);
		ConverterAndOverridesHelper.INSTANCE.processAssociationOverrides(
				getPath(),
				backingMember,
				container,
				getContext()
		);

		validateMapping();
	}

	private CollectionIdInformation extractCollectionIdInformation(MemberDescriptor backingMember) {
		final AnnotationInstance collectionId = backingMember.getAnnotations().get( HibernateDotNames.COLLECTION_ID );
		if ( collectionId == null ) {
			return null;
		}

		final IdentifierGeneratorDefinition generator = new IdentifierGeneratorDefinition(
				null,
				collectionId.value( "generator" ).asString(),
				null
		);

		final AnnotationInstance type = JandexHelper.getValue( collectionId, "type", AnnotationInstance.class,
				classLoaderService );
		final AnnotationValue typeType = type.value( "type" );
		final String typeName = typeType == null ? null : typeType.asString();
		if ( StringHelper.isEmpty( typeName ) ) {
			throw getContext().makeMappingException(
					"Plural attribute [" + backingMember.toString() + "] specified @CollectionId.type incorrectly, " +
							"type name was missing"
			);
		}
		final ExplicitTypeResolver typeResolver = new ExplicitTypeResolver( typeName, extractTypeParameters( type ) );

		final AnnotationInstance[] columnAnnotations = JandexHelper.getValue(
				collectionId,
				"columns",
				AnnotationInstance[].class,
				classLoaderService
		);
		final List<Column> idColumns = CollectionHelper.arrayList( columnAnnotations.length );
		for ( AnnotationInstance columnAnnotation : columnAnnotations ) {
			idColumns.add( new Column( columnAnnotation ) );
		}

		return new CollectionIdInformationImpl( idColumns, typeResolver, generator );
	}

	private Map<String, String> extractTypeParameters(AnnotationInstance type) {
		final AnnotationInstance[] paramAnnotations = JandexHelper.getValue(
				type,
				"parameters",
				AnnotationInstance[].class,
				classLoaderService
		);
		if ( paramAnnotations == null || paramAnnotations.length == 0 ) {
			return Collections.emptyMap();
		}
		else if ( paramAnnotations.length == 1 ) {
			return Collections.singletonMap(
					paramAnnotations[0].value( "name" ).asString(),
					paramAnnotations[0].value( "value" ).asString()
			);
		}

		final Map<String,String> parameters = CollectionHelper.mapOfSize( paramAnnotations.length );
		for ( AnnotationInstance paramAnnotation : paramAnnotations ) {
			parameters.put(
					paramAnnotation.value( "name" ).asString(),
					paramAnnotation.value( "value" ).asString()
			);
		}
		return parameters;
	}


	private PluralAttributeNature resolvePluralAttributeNature(
			MemberDescriptor backingMember,
			CollectionIdInformation collectionIdInformation) {
		//TODO org.hibernate.cfg.annotations.CollectionBinder#hasToBeSorted

		final JavaTypeDescriptor pluralType = backingMember.getType().getErasedType();

		if ( ArrayDescriptor.class.isInstance( pluralType ) ) {
			return PluralAttributeNature.ARRAY;
		}

		if ( getContext().getJavaTypeDescriptorRepository().jdkMapDescriptor().isAssignableFrom( pluralType ) ) {
			return PluralAttributeNature.MAP;
		}

		if ( getContext().getJavaTypeDescriptorRepository().jdkSetDescriptor().isAssignableFrom( pluralType ) ) {
			return PluralAttributeNature.SET;
		}

		if ( getContext().getJavaTypeDescriptorRepository().jdkListDescriptor().isAssignableFrom( pluralType ) ) {
			// we have a LIST nature as long as there is an @OrderColumn annotation
			if ( backingMember.getAnnotations().containsKey( ORDER_COLUMN ) ) {
				return PluralAttributeNature.LIST;
			}
		}

		// just a double check...
		if ( !getContext().getJavaTypeDescriptorRepository().jdkCollectionDescriptor().isAssignableFrom( pluralType ) ) {
			throw getContext().makeMappingException(
					"Plural attribute [" + backingMember.toString() + "] was not a collection or an array"
			);
		}

		// at this point we have either a BAG or IDBAG, depending on whether
		// there is a @CollectionId present

		// todo : does ID_BAG really need a separate nature?
		return collectionIdInformation != null
				? PluralAttributeNature.ID_BAG
				: PluralAttributeNature.BAG;
	}

	private PluralAttributeElementDetails resolveElementDetails(
			MemberDescriptor backingMember,
			Nature attributeNature,
			JavaTypeDescriptor elementType) {
		switch ( attributeNature ) {
			case ELEMENT_COLLECTION_BASIC: {
				return new PluralAttributeElementDetailsBasic( this, elementType );
			}
			case ELEMENT_COLLECTION_EMBEDDABLE: {
				return new PluralAttributeElementDetailsEmbedded( this, elementType );
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				return new PluralAttributeElementDetailsEntity( this, elementType );
			}
			case MANY_TO_ANY: {
				throw new NotYetImplementedException( "@ManyToAny element support still baking" );
			}
			default: {
				throw getContext().makeMappingException(
						"Unexpected nature encountered for plural attribute : " + attributeNature
				);
			}
		}
	}

	private PluralAttributeIndexDetails resolveIndexDetails(
			MemberDescriptor backingMember,
			PluralAttributeNature pluralAttributeNature,
			JavaTypeDescriptor indexType) {
		// could be an array/list
		if ( pluralAttributeNature == PluralAttributeNature.ARRAY
				|| pluralAttributeNature == PluralAttributeNature.LIST ) {
			return new PluralAttributeIndexDetailsSequential( this, backingMember );
		}

		// or a map
		if ( pluralAttributeNature != PluralAttributeNature.MAP ) {
			return null;
		}

		final AnnotationInstance mapKeyAnnotation = backingMember.getAnnotations().get( MAP_KEY );
		final AnnotationInstance mapKeyClassAnnotation = backingMember.getAnnotations().get( MAP_KEY_CLASS );
		final AnnotationInstance mapKeyColumnAnnotation = backingMember.getAnnotations().get( MAP_KEY_COLUMN );
		final AnnotationInstance mapKeyEnumeratedAnnotation = backingMember.getAnnotations().get( MAP_KEY_ENUMERATED );
		final AnnotationInstance mapKeyTemporalAnnotation = backingMember.getAnnotations().get( MAP_KEY_TEMPORAL );
		final AnnotationInstance mapKeyTypeAnnotation = backingMember.getAnnotations().get( HibernateDotNames.MAP_KEY_TYPE);

		final List<AnnotationInstance> mapKeyJoinColumnAnnotations = collectMapKeyJoinColumnAnnotations( backingMember );

		if ( mapKeyAnnotation != null && mapKeyClassAnnotation != null ) {
			// this is an error according to the spec...
			throw getContext().makeMappingException(
					"Map attribute defined both @MapKey and @MapKeyClass; only one should be used : " +
							backingMember.toLoggableForm()
			);
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// @MapKey

		if ( mapKeyAnnotation != null ) {
			final AnnotationValue value = mapKeyAnnotation.value( "name" );
			String mapKeyAttributeName = null;
			if ( value != null ) {
				mapKeyAttributeName = StringHelper.nullIfEmpty( value.asString() );
			}
			return new PluralAttributeIndexDetailsMapKeyEntityAttribute( this, backingMember, indexType, mapKeyAttributeName );
		}



		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// @MapKeyEnumerated / @MapKeyTemporal imply basic key

		if ( mapKeyEnumeratedAnnotation != null || mapKeyTemporalAnnotation != null ) {
			return new PluralAttributeIndexDetailsMapKeyBasic( this, backingMember, indexType, mapKeyColumnAnnotation );
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// if we could not decode a specific key type, we assume basic

		JavaTypeDescriptor mapKeyType = indexType;
		if ( mapKeyClassAnnotation != null ) {
			final DotName name = mapKeyClassAnnotation.value().asClass().name();
			mapKeyType = getContext().getJavaTypeDescriptorRepository().getType( name );
		}
		if (mapKeyType == null && mapKeyTypeAnnotation != null) {
			final AnnotationInstance typeAnnotation = JandexHelper.getValue( mapKeyTypeAnnotation, "value",
					AnnotationInstance.class, classLoaderService );
			final DotName name = DotName.createSimple( typeAnnotation.value( "type" ).asString() );
			mapKeyType = getContext().getJavaTypeDescriptorRepository().getType( name );
		}
		if ( mapKeyType == null ) {
			if ( !mapKeyJoinColumnAnnotations.isEmpty() ) {
				throw getContext().makeMappingException(
						"Map key type could not be resolved (to determine entity name to use as key), " +
								"but @MapKeyJoinColumn(s) was present.  Map should either use generics or " +
								"use @MapKeyClass/@MapKeyType to specify entity class"
				);
			}
			return new PluralAttributeIndexDetailsMapKeyBasic( this, backingMember, indexType, mapKeyColumnAnnotation );
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Level 4 : if @MapKeyJoinColumn(s) were specified, we have an entity

		if ( !mapKeyJoinColumnAnnotations.isEmpty() ) {
			throw new NotYetImplementedException( "Entities as map keys not yet implemented" );
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Level 5 : if decode the nature of the map key type

		if ( mapKeyType.findTypeAnnotation( EMBEDDABLE ) != null ) {
			return new PluralAttributeIndexDetailsMapKeyEmbedded( this, backingMember, indexType );
		}

		if ( mapKeyType.findTypeAnnotation( ENTITY ) != null ) {
			throw new NotYetImplementedException( "Entities as map keys not yet implemented" );
		}

		return new PluralAttributeIndexDetailsMapKeyBasic( this, backingMember, mapKeyType, mapKeyColumnAnnotation );
	}

	private List<AnnotationInstance> collectMapKeyJoinColumnAnnotations(MemberDescriptor backingMember) {
		final AnnotationInstance singular = backingMember.getAnnotations().get( JPADotNames.MAP_KEY_JOIN_COLUMN );
		final AnnotationInstance plural = backingMember.getAnnotations().get( JPADotNames.MAP_KEY_JOIN_COLUMNS );

		if ( singular != null && plural != null ) {
			throw getContext().makeMappingException(
					"Attribute [" + backingMember.toLoggableForm() +
							"] declared both @MapKeyJoinColumn and " +
							"@MapKeyJoinColumns; should only use one or the other"
			);
		}

		if ( singular == null && plural == null ) {
			return Collections.emptyList();
		}

		if ( singular != null ) {
			return Collections.singletonList( singular );
		}

		final AnnotationInstance[] annotations = JandexHelper.extractAnnotationsValue(
				plural,
				"value"
		);
		if ( annotations == null || annotations.length == 0 ) {
			return null;
		}

		return Arrays.asList( annotations );
	}

	private Caching determineCachingSettings(MemberDescriptor backingMember) {
		Caching caching = new Caching( TruthValue.UNKNOWN );

		final AnnotationInstance hbmCacheAnnotation = backingMember.getAnnotations().get( HibernateDotNames.CACHE );
		if ( hbmCacheAnnotation != null ) {
			caching.setRequested( TruthValue.TRUE );

			final AnnotationValue usageValue = hbmCacheAnnotation.value( "usage" );
			if ( usageValue != null ) {
				caching.setAccessType( CacheConcurrencyStrategy.parse( usageValue.asEnum() ).toAccessType() );
			}

			final AnnotationValue regionValue = hbmCacheAnnotation.value( "region" );
			if ( regionValue != null ) {
				caching.setRegion( regionValue.asString() );
			}

			// NOTE "include" is irrelevant for collections
		}

		return caching;
	}

	private boolean determineWhetherIsExtraLazy(AnnotationInstance lazyCollectionAnnotation) {
		if ( lazyCollectionAnnotation == null ) {
			return false;
		}
		final AnnotationValue optionValue = lazyCollectionAnnotation.value( "value" );
		if ( optionValue == null ) {
			return false;
		}
		final LazyCollectionOption option = LazyCollectionOption.valueOf( optionValue.asEnum() );
		return option == LazyCollectionOption.EXTRA;
	}

	private String determineCustomLoaderName(MemberDescriptor backingMember) {
		final AnnotationInstance loaderAnnotation = backingMember.getAnnotations().get( LOADER );
		return loaderAnnotation == null
				? null
				: StringHelper.nullIfEmpty( loaderAnnotation.value( "namedQuery" ).asString() );
	}

	private String determineCustomPersister(MemberDescriptor backingMember) {
		final AnnotationInstance persisterAnnotation = backingMember.getAnnotations().get( PERSISTER );
		return persisterAnnotation == null
				? null
				: StringHelper.nullIfEmpty( persisterAnnotation.value( "impl" ).asString() );
	}

	private OnDeleteAction determineOnDeleteAction(MemberDescriptor backingMember) {
		final AnnotationInstance onDeleteAnnotation = backingMember.getAnnotations().get(
				ON_DELETE
		);
		return onDeleteAnnotation == null
				? null
				: OnDeleteAction.valueOf( onDeleteAnnotation.value( "action" ).asString() );
	}

	private String determineWereClause(MemberDescriptor backingMember) {
		final AnnotationInstance whereAnnotation = backingMember.getAnnotations().get( WHERE );
		return whereAnnotation == null
				? null
				: StringHelper.nullIfEmpty( whereAnnotation.value( "clause" ).asString() );
	}

	private String determineOrderBy(MemberDescriptor backingMember) {
		final AnnotationInstance hbmOrderBy = backingMember.getAnnotations().get( HibernateDotNames.ORDER_BY );
		final AnnotationInstance jpaOrderBy = backingMember.getAnnotations().get( ORDER_BY );

		if ( hbmOrderBy != null && jpaOrderBy != null ) {
			throw getContext().makeMappingException(
					"Cannot use sql order by clause (@org.hibernate.annotations.OrderBy) " +
							"in conjunction with JPA order by clause (@java.persistence.OrderBy) on  " +
							backingMember.toString()
			);
		}


		if ( hbmOrderBy != null ) {
			return StringHelper.nullIfEmpty( hbmOrderBy.value( "clause" ).asString() );
		}

		if ( jpaOrderBy != null ) {
			// this could be an empty string according to JPA spec 11.1.38 -
			// If the ordering element is not specified for an entity association, ordering by the primary key of the
			// associated entity is assumed
			// The binder will need to take this into account and generate the right property names
			final AnnotationValue orderByValue = jpaOrderBy.value();
			final String value = orderByValue == null ? null : StringHelper.nullIfEmpty( orderByValue.asString() );
			if ( value == null || value.equalsIgnoreCase( "asc" ) ) {
				return isBasicCollection() ?  "$element$ asc" : "id asc" ;
			}
			else if ( value.equalsIgnoreCase( "desc" ) ) {
				return isBasicCollection() ? "$element$ desc" : "id desc";
			}
			else {
				return value;
			}
		}

		return null;
	}

	private void validateMapping() {
		checkSortedTypeIsSortable();
		checkIfCollectionIdIsWronglyPlaced();
	}

	private void checkIfCollectionIdIsWronglyPlaced() {
		if ( collectionIdInformation != null && CANNOT_HAVE_COLLECTION_ID.contains( pluralAttributeNature ) ) {
			throw new MappingException(
					"The Collection type doesn't support @CollectionId annotation: " + getRole(),
					getContext().getOrigin()
			);
		}
	}

	private void checkSortedTypeIsSortable() {
		if ( pluralAttributeNature != PluralAttributeNature.MAP
				&& pluralAttributeNature != PluralAttributeNature.SET ) {
			return;
		}

		final JavaTypeDescriptor sortedMapType = getContext().getJavaTypeDescriptorRepository().getType(
				DotName.createSimple( SortedMap.class.getName() )
		);
		final JavaTypeDescriptor sortedSetType = getContext().getJavaTypeDescriptorRepository().getType(
				DotName.createSimple( SortedSet.class.getName() )
		);
		if ( sortedMapType.isAssignableFrom( getBackingMember().getType().getErasedType() )
				|| sortedSetType.isAssignableFrom( getBackingMember().getType().getErasedType() ) ) {
			if ( !isSorted() ) {
				throw getContext().makeMappingException(
						"A SortedSet/SortedMap attribute has to define @SortNatural or @SortComparator : " +
								getBackingMember().toString()
				);
			}
		}
	}

	public AnnotationInstance getJoinTableAnnotation() {
		return joinTableAnnotation;
	}

	public ArrayList<Column> getJoinColumnValues() {
		return joinColumnValues;
	}

	public ArrayList<Column> getInverseJoinColumnValues() {
		return inverseJoinColumnValues;
	}

	@Override
	public boolean isOptional() {
		return isOptional;
	}

	@Override
	public boolean isInsertable() {
		// irrelevant
		return true;
	}

	@Override
	public boolean isUpdatable() {
		// irrelevant
		return true;
	}

	@Override
	public PropertyGeneration getPropertyGeneration() {
		return PropertyGeneration.NEVER;
	}

	@Override
	public String getMappedByAttributeName() {
		return mappedByAttributeName;
	}

	public void setMappedByAttributeName(String mappedByAttributeName) {
		this.mappedByAttributeName = mappedByAttributeName;
	}

	@Override
	public boolean isInverse() {
		return isInverse;
	}

	@Override
	public Set<CascadeType> getJpaCascadeTypes() {
		return jpaCascadeTypes;
	}

	@Override
	public Set<org.hibernate.annotations.CascadeType> getHibernateCascadeTypes() {
		return hibernateCascadeTypes;
	}

	@Override
	public boolean isOrphanRemoval() {
		return isOrphanRemoval;
	}

	@Override
	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	@Override
	public boolean isUnWrapProxy() {
		return isUnWrapProxy;
	}

	@Override
	public FetchStyle getFetchStyle() {
		return fetchStyle;
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	public CollectionIdInformation getCollectionIdInformation() {
		return collectionIdInformation;
	}

	public PluralAttributeElementDetails getElementDetails() {
		return elementDetails;
	}

	public PluralAttributeIndexDetails getIndexDetails() {
		return indexDetails;
	}

	public PluralAttributeNature getPluralAttributeNature() {
		return pluralAttributeNature;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public String getInverseForeignKeyName() {
		return foreignKeyDelegate.getInverseForeignKeyName();
	}
	public String getExplicitForeignKeyName(){
		return foreignKeyDelegate.getExplicitForeignKeyName();
	}
	public boolean createForeignKeyConstraint(){
		return foreignKeyDelegate.createForeignKeyConstraint();
 	}

	public Caching getCaching() {
		return caching;
	}

	public String getCustomPersister() {
		return customPersister;
	}

	public String getCustomLoaderName() {
		return customLoaderName;
	}

	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	public CustomSQL getCustomDeleteAll() {
		return customDeleteAll;
	}

	@Override
	public String toString() {
		return "PluralAttribute{name='" + getRole().getFullPath() + '\'' + '}';
	}
	public OnDeleteAction getOnDeleteAction() {
		return onDeleteAction;
	}

	public String getComparatorName() {
		return comparatorName;
	}

	public boolean isSorted() {
		return sorted;
	}




	@Override
	public boolean isIncludeInOptimisticLocking() {
		return hasOptimisticLockAnnotation()
				? super.isIncludeInOptimisticLocking()
				: !isInverse;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public boolean isExtraLazy() {
		return isExtraLazy;
	}

	private boolean isBasicCollection(){
		return getNature() == Nature.ELEMENT_COLLECTION_BASIC || getNature() == Nature.ELEMENT_COLLECTION_EMBEDDABLE;
	}


	public boolean isMutable() {
		return mutable;
	}


	@Override
	public AttributeTypeResolver getHibernateTypeResolver() {
		return collectionTypeResolver;
	}

//	public AttributeTypeResolver getIndexTypeResolver() {
//		if ( indexType == null ) {
//				return getDefaultHibernateTypeResolver();
//		}
//		else if ( indexTypeResolver == null ) {
//			CompositeAttributeTypeResolver resolver = new CompositeAttributeTypeResolver( this );
//			final String name = getName() + ".index";
//			resolver.addHibernateTypeResolver( HibernateTypeResolver.createCollectionIndexTypeResolver( this ) );
//			// TODO: Lob allowed as collection index? I don't see an annotation for that.
//			resolver.addHibernateTypeResolver( EnumeratedTypeResolver.createCollectionIndexTypeResolver( this ) );
//			resolver.addHibernateTypeResolver( TemporalTypeResolver.createCollectionIndexTypeResolver( this ) );
//			indexTypeResolver = resolver;
//		}
//		return indexTypeResolver;
//	}
}


