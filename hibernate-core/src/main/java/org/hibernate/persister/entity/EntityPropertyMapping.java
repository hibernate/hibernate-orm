/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.Remove;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.SpecialOneToOneType;
import org.hibernate.type.Type;
import org.hibernate.type.MappingContext;

/**
 * @author Gavin King
 *
 * @deprecated Replaced by {@link org.hibernate.metamodel.mapping.EntityMappingType}
 */
@Deprecated(since = "6", forRemoval = true)
@Remove
class EntityPropertyMapping {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( EntityPropertyMapping.class );

	private final Map<String, Type> typesByPropertyPath = new HashMap<>();
	private final AbstractEntityPersister persister;

	public EntityPropertyMapping(AbstractEntityPersister persister) {
		this.persister = persister;
	}

	public String[] getIdentifierColumnNames() {
		return persister.getIdentifierColumnNames();
	}

	public String[] getIdentifierColumnReaders() {
		return persister.getIdentifierColumnReaders();
	}

	public String[] getIdentifierColumnReaderTemplates() {
		return persister.getIdentifierColumnReaderTemplates();
	}

	protected String getEntityName() {
		return persister.getEntityName();
	}

	//This field is only used during initialization, no need for threadsafety:
	//FIXME get rid of the field, or at least clear it after boot?
	//      Not urgent as we typically won't initialize it at all.
	private Set<String> duplicateIncompatiblePaths = null;

	private final Map<String, String[]> columnsByPropertyPath = new HashMap<>();
	private final Map<String, String[]> columnReadersByPropertyPath = new HashMap<>();
	private final Map<String, String[]> columnReaderTemplatesByPropertyPath = new HashMap<>();

	public Type toType(String propertyName) throws QueryException {
		Type type = typesByPropertyPath.get( propertyName );
		if ( type == null ) {
			throw propertyException( propertyName );
		}
		return type;
	}

	protected final QueryException propertyException(String propertyName) throws QueryException {
		return new QueryException( "Could not resolve property: " + propertyName + " of: " + getEntityName() );
	}

	public String[] getColumnNames(String propertyName) {
		String[] cols = columnsByPropertyPath.get( propertyName );
		if ( cols == null ) {
			throw new MappingException( "Unknown property: " + propertyName );
		}
		return cols;
	}

	private void logDuplicateRegistration(String path, Type existingType, Type type) {
		// Disabled because this resulted in many useless messages
//		if ( LOG.isTraceEnabled() ) {
//			LOG.tracev(
//					"Skipping duplicate registration of path [{0}], existing type = [{1}], incoming type = [{2}]",
//					path,
//					existingType,
//					type
//			);
//		}
	}

	private void logIncompatibleRegistration(String path, Type existingType, Type type) {
		if ( log.isTraceEnabled() ) {
			log.tracev(
					"Skipped adding attribute [{1}] to base type [{0}] as more than one subtype defined the attribute using incompatible types (strictly speaking the attributes are not inherited); existing type = [{2}], incoming type = [{3}]",
					getEntityName(),
					path,
					existingType,
					type
			);
		}
	}

	protected void addPropertyPath(
			String path,
			Type type,
			String[] columns,
			String[] columnReaders,
			String[] columnReaderTemplates,
			Metadata factory) {
		Type existingType = typesByPropertyPath.get( path );
		if ( existingType != null || ( duplicateIncompatiblePaths != null && duplicateIncompatiblePaths.contains( path ) ) ) {
			// If types match or the new type is not an association type, there is nothing for us to do
			if ( type == existingType || existingType == null || !( type instanceof AssociationType ) ) {
				logDuplicateRegistration( path, existingType, type );
			}
			else if ( !( existingType instanceof AssociationType ) ) {
				// Workaround for org.hibernate.cfg.annotations.PropertyBinder#bind() adding a component for *ToOne ids
				logDuplicateRegistration( path, existingType, type );
			}
			else {
				if ( type instanceof AnyType && existingType instanceof AnyType ) {
					// TODO: not sure how to handle any types. For now we just return and let the first type dictate what type the property has...
				}
				else {
					Type commonType = null;
					MetadataImplementor metadata = (MetadataImplementor) factory;
					if ( type instanceof CollectionType && existingType instanceof CollectionType ) {
						Collection thisCollection = metadata.getCollectionBinding( ( (CollectionType) existingType ).getRole() );
						Collection otherCollection = metadata.getCollectionBinding( ( (CollectionType) type ).getRole() );

						if ( thisCollection.isSame( otherCollection ) ) {
							logDuplicateRegistration( path, existingType, type );
							return;
						}
						else {
							logIncompatibleRegistration( path, existingType, type );
						}
					}
					else if ( type instanceof EntityType entityType2 && existingType instanceof EntityType entityType1 ) {

						if ( entityType1.getAssociatedEntityName().equals( entityType2.getAssociatedEntityName() ) ) {
							logDuplicateRegistration( path, existingType, type );
							return;
						}
						else {
							commonType = getCommonType( metadata, entityType1, entityType2 );
						}
					}
					else {
						logIncompatibleRegistration( path, existingType, type );
					}
					if ( commonType == null ) {
						if ( duplicateIncompatiblePaths == null ) {
							duplicateIncompatiblePaths = new HashSet<>();
						}
						duplicateIncompatiblePaths.add( path );
						typesByPropertyPath.remove( path );
						// Set everything to empty to signal action has to be taken!
						// org.hibernate.hql.internal.ast.tree.DotNode#dereferenceEntityJoin() is reacting to this
						String[] empty = ArrayHelper.EMPTY_STRING_ARRAY;
						columnsByPropertyPath.put( path, empty );
						columnReadersByPropertyPath.put( path, empty );
						columnReaderTemplatesByPropertyPath.put( path, empty );
					}
					else {
						typesByPropertyPath.put( path, commonType );
					}
				}
			}
		}
		else {
			typesByPropertyPath.put( path, type );
			columnsByPropertyPath.put( path, columns );
			columnReadersByPropertyPath.put( path, columnReaders );
			columnReaderTemplatesByPropertyPath.put( path, columnReaderTemplates );
		}
	}

	private Type getCommonType(MetadataImplementor metadata, EntityType entityType1, EntityType entityType2) {
		PersistentClass thisClass = metadata.getEntityBinding( entityType1.getAssociatedEntityName() );
		PersistentClass otherClass = metadata.getEntityBinding( entityType2.getAssociatedEntityName() );
		PersistentClass commonClass = getCommonPersistentClass( thisClass, otherClass );

		if ( commonClass == null ) {
			return null;
		}

		// Create a copy of the type but with the common class
		if ( entityType1 instanceof ManyToOneType manyToOneType ) {
			return new ManyToOneType( manyToOneType, commonClass.getEntityName() );
		}
		else if ( entityType1 instanceof SpecialOneToOneType specialOneToOneType ) {
			return new SpecialOneToOneType( specialOneToOneType, commonClass.getEntityName() );
		}
		else if ( entityType1 instanceof OneToOneType oneToOneType ) {
			return new OneToOneType( oneToOneType, commonClass.getEntityName() );
		}
		else {
			throw new IllegalStateException( "Unexpected entity type: " + entityType1 );
		}
	}

	private PersistentClass getCommonPersistentClass(PersistentClass clazz1, PersistentClass clazz2) {
		while ( clazz2 != null && clazz2.getMappedClass() != null && clazz1.getMappedClass() != null && !clazz2.getMappedClass()
				.isAssignableFrom( clazz1.getMappedClass() ) ) {
			clazz2 = clazz2.getSuperclass();
		}
		return clazz2;
	}

	protected void initPropertyPaths(
			final String path,
			final Type type,
			String[] columns,
			String[] columnReaders,
			String[] columnReaderTemplates,
			final String[] formulaTemplates,
			final Metadata factory) throws MappingException {
		assert columns != null : "Incoming columns should not be null : " + path;
		assert type != null : "Incoming type should not be null : " + path;

		if ( columns.length != type.getColumnSpan( factory ) ) {
			throw new MappingException(
					"broken column mapping for: " + path +
							" of: " + getEntityName()
			);
		}

		if ( type instanceof AnyType || type instanceof CollectionType || type instanceof EntityType ) {
			AssociationType actype = (AssociationType) type;
			if ( actype.useLHSPrimaryKey() ) {
				columns = getIdentifierColumnNames();
				columnReaders = getIdentifierColumnReaders();
				columnReaderTemplates = getIdentifierColumnReaderTemplates();
			}
			else {
				String foreignKeyProperty = actype.getLHSPropertyName();
				if ( foreignKeyProperty != null && !path.equals( foreignKeyProperty ) ) {
					//TODO: this requires that the collection is defined after the
					//      referenced property in the mapping file (ok?)
					columns = columnsByPropertyPath.get( foreignKeyProperty );
					if ( columns == null ) {
						return; //get 'em on the second pass!
					}
					columnReaders = columnReadersByPropertyPath.get( foreignKeyProperty );
					columnReaderTemplates = columnReaderTemplatesByPropertyPath.get( foreignKeyProperty );
				}
			}
		}

		if ( path != null ) {
			addPropertyPath( path, type, columns, columnReaders, columnReaderTemplates, factory );
		}

		if ( type instanceof AnyType actype ) {
			initComponentPropertyPaths(
					path,
					actype,
					columns,
					columnReaders,
					columnReaderTemplates,
					formulaTemplates,
					factory
			);
		}
		else if ( type instanceof ComponentType actype ) {
			initComponentPropertyPaths(
					path,
					actype,
					columns,
					columnReaders,
					columnReaderTemplates,
					formulaTemplates,
					factory
			);
			if ( actype.isEmbedded() ) {
				initComponentPropertyPaths(
						path == null ? null : StringHelper.qualifier( path ),
						actype,
						columns,
						columnReaders,
						columnReaderTemplates,
						formulaTemplates,
						factory
				);
			}
		}
		else if ( type instanceof EntityType ) {
			initIdentifierPropertyPaths(
					path,
					(EntityType) type,
					columns,
					columnReaders,
					columnReaderTemplates,
					formulaTemplates != null && formulaTemplates.length > 0 ? formulaTemplates : null,
					factory
			);
		}
	}

	protected void initIdentifierPropertyPaths(
			final String path,
			final EntityType etype,
			final String[] columns,
			final String[] columnReaders,
			final String[] columnReaderTemplates,
			final String[] formulaTemplates,
			final Metadata factory) throws MappingException {

		Type idtype = etype.getIdentifierOrUniqueKeyType( factory );
		String idPropName = etype.getIdentifierOrUniqueKeyPropertyName( factory );
		boolean hasNonIdentifierPropertyNamedId = hasNonIdentifierPropertyNamedId( etype, factory );

		if ( etype.isReferenceToPrimaryKey() ) {
			if ( !hasNonIdentifierPropertyNamedId ) {
				String idpath1 = extendPath( path, EntityPersister.ENTITY_ID );
				addPropertyPath( idpath1, idtype, columns, columnReaders, columnReaderTemplates, factory );
				initPropertyPaths( idpath1, idtype, columns, columnReaders, columnReaderTemplates, formulaTemplates, factory );
			}
		}

		if ( !etype.isNullable() && idPropName != null ) {
			String idpath2 = extendPath( path, idPropName );
			addPropertyPath( idpath2, idtype, columns, columnReaders, columnReaderTemplates, factory );
			initPropertyPaths( idpath2, idtype, columns, columnReaders, columnReaderTemplates, formulaTemplates, factory );
		}
	}

	private boolean hasNonIdentifierPropertyNamedId(final EntityType entityType, final MappingContext factory) {
		// TODO : would be great to have a Mapping#hasNonIdentifierPropertyNamedId method
		// I don't believe that Mapping#getReferencedPropertyType accounts for the identifier property; so
		// if it returns for a property named 'id', then we should have a non-id field named id
		try {
			return factory.getReferencedPropertyType(
					entityType.getAssociatedEntityName(),
					EntityPersister.ENTITY_ID
			) != null;
		}
		catch (MappingException e) {
			return false;
		}
	}

	protected void initComponentPropertyPaths(
			final String path,
			final CompositeType type,
			final String[] columns,
			final String[] columnReaders,
			final String[] columnReaderTemplates,
			final String[] formulaTemplates,
			final Metadata factory) throws MappingException {

		Type[] types = type.getSubtypes();
		String[] properties = type.getPropertyNames();
		int begin = 0;
		for ( int i = 0; i < properties.length; i++ ) {
			String subpath = extendPath( path, properties[i] );
			try {
				int length = types[i].getColumnSpan( factory );
				String[] columnSlice = ArrayHelper.slice( columns, begin, length );
				String[] columnReaderSlice = ArrayHelper.slice( columnReaders, begin, length );
				String[] columnReaderTemplateSlice = ArrayHelper.slice( columnReaderTemplates, begin, length );
				String[] formulaSlice = formulaTemplates == null ?
						null : ArrayHelper.slice( formulaTemplates, begin, length );
				initPropertyPaths(
						subpath,
						types[i],
						columnSlice,
						columnReaderSlice,
						columnReaderTemplateSlice,
						formulaSlice,
						factory
				);
				begin += length;
			}
			catch (Exception e) {
				throw new MappingException( "bug in initComponentPropertyPaths", e );
			}
		}
	}

	private static String extendPath(String path, String property) {
		return StringHelper.isEmpty( path ) ? property : StringHelper.qualify( path, property );
	}
}
