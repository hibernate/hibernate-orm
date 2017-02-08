/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.AbstractPersistentAttribute;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.SingularPersistentAttribute.Disposition;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.persister.common.spi.UnionSubclassTable;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.IdentifiableTypeImplementor;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sqm.domain.SqmPluralAttribute.CollectionClassification;
import org.hibernate.sqm.domain.SqmPluralAttributeElement.ElementClassification;
import org.hibernate.sqm.domain.SqmSingularAttribute.SingularAttributeClassification;
import org.hibernate.sqm.query.SqmPropertyPath;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.internal.EntityTypeImpl;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.EmbeddedType;
import org.hibernate.type.spi.EntityType;
import org.hibernate.type.spi.Type;

/**
 * For now mainly a helper for reflection into stuff not exposed on the entity/collection persister
 * contracts
 *
 * @author Steve Ebersole
 */
public class PersisterHelper {

	public static org.hibernate.loader.PropertyPath convert(SqmPropertyPath propertyPath) {
		if ( propertyPath.getParent() == null ) {
			return new org.hibernate.loader.PropertyPath( null, propertyPath.getLocalPath() );
		}
		org.hibernate.loader.PropertyPath parent = convert( propertyPath.getParent() );
		return parent.append( propertyPath.getLocalPath() );
	}






	private final Method subclassTableSpanMethod;
	private final Method subclassPropertyTableNumberMethod;
	private final Method subclassPropertyColumnsMethod;
	private final Method subclassPropertyFormulasMethod;

	/**
	 * Singleton access
	 */
	public static final PersisterHelper INSTANCE = new PersisterHelper();

	private PersisterHelper() {
		try {
			subclassTableSpanMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassTableSpan" );
			subclassTableSpanMethod.setAccessible( true );

			subclassPropertyTableNumberMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassPropertyTableNumber", int.class );
			subclassPropertyTableNumberMethod.setAccessible( true );

			subclassPropertyColumnsMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassPropertyColumnReaderClosure" );
			subclassPropertyColumnsMethod.setAccessible( true );

			subclassPropertyFormulasMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassPropertyFormulaTemplateClosure" );
			subclassPropertyFormulasMethod.setAccessible( true );
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to initialize access to AbstractEntityPersister#getSubclassTableSpan", e );
		}
	}

	public int extractSubclassTableCount(EntityPersister persister) {
		try {
			return (Integer) subclassTableSpanMethod.invoke( persister );
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassTableSpan [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassTableSpan [" + persister.toString() + "]",
					e
			);
		}
	}

	public int getSubclassPropertyTableNumber(EntityPersister persister, int subclassPropertyNumber) {
		try {
			return (Integer) subclassPropertyTableNumberMethod.invoke( persister, subclassPropertyNumber );
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e
			);
		}
	}

	public Table getPropertyTable(EntityPersister persister, String attributeName, Table[] tables) {
		final String tableName = ( (OuterJoinLoadable) persister ).getPropertyTableName( attributeName );
		for ( Table table : tables ) {
			if ( table instanceof UnionSubclassTable ) {
				if ( ( (UnionSubclassTable) table ).includes( tableName ) ) {
					return table;
				}
			}
			if ( table.getTableExpression().equals( tableName ) ) {
				return table;
			}
		}
		throw new HibernateException(
				"Could not locate Table for attribute [" + persister.getEntityName() + ".'" + attributeName + "]"
		);
	}

	public String[] getSubclassPropertyColumnExpressions(EntityPersister persister, int subclassPropertyNumber) {
		try {
			final String[][] columnExpressions = (String[][]) subclassPropertyColumnsMethod.invoke( persister );
			return columnExpressions[subclassPropertyNumber];
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e
			);
		}
	}

	public String[] getSubclassPropertyFormulaExpressions(EntityPersister persister, int subclassPropertyNumber) {
		try {
			final String[][] columnExpressions = (String[][]) subclassPropertyFormulasMethod.invoke( persister );
			return columnExpressions[subclassPropertyNumber];
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e
			);
		}
	}

	public static List<Column> makeValues(
			SessionFactoryImplementor factory,
			Type type,
			String[] columns,
			String[] formulas,
			Table table) {
		assert formulas == null || columns.length == formulas.length;

		final List<Column> values = new ArrayList<>();

		for ( int i = 0; i < columns.length; i++ ) {
			final int jdbcType = type.sqlTypes()[i];

			if ( columns[i] != null ) {
				values.add( table.makeColumn( columns[i], jdbcType ) );
			}
			else {
				if ( formulas == null ) {
					throw new IllegalStateException( "Column name was null and no formula information was supplied" );
				}
				values.add( table.makeFormula( formulas[i], jdbcType ) );
			}
		}

		return values;
	}

	public PersistentAttribute buildAttribute(
			PersisterCreationContext creationContext,
			ManagedTypeImplementor source,
			Property property,
			List<Column> columns) {
		if ( property.getValue() instanceof Collection ) {
			assert columns == null || columns.isEmpty();

			return buildPluralAttribute(
					creationContext,
					source,
					property
			);
		}
		else {
			return buildSingularAttribute(
					creationContext,
					source,
					property,
					columns
			);
		}
	}

	public AbstractPersistentAttribute buildSingularAttribute(
			PersisterCreationContext creationContext,
			ManagedTypeImplementor source,
			Property property,
			List<Column> columns) {
		if ( property.getValue() instanceof Any ) {
			throw new NotYetImplementedException();
		}
		else if ( property.getValue() instanceof Component ) {
			return new SingularPersistentAttributeEmbedded(
					source,
					property.getName(),
					resolvePropertyAccess( creationContext, property ),
					Disposition.NORMAL,
					creationContext.getPersisterFactory().createEmbeddablePersister(
							(Component) property.getValue(),
							source,
							property.getName(),
							creationContext
					)
			);
		}
		else if ( property.getValue() instanceof ToOne ) {
			final ToOne toOne = (ToOne) property.getValue();

			if ( property.getValue() instanceof OneToOne ) {
				// the Classification here should be ONE_TO_ONE which could represent either a real PK one-to-one
				//		or a unique-FK one-to-one (logical).  If this is a real one-to-one then we should have
				//		no columns passed here and should instead use the LHS (source) PK column(s)
				assert columns == null || columns.size() == 0;
				columns = ( (EntityPersister) source ).getHierarchy().getIdentifierDescriptor().getColumns();
			}
			assert columns != null && columns.size() > 0;

			return new SingularPersistentAttributeEntity(
					source,
					property.getName(),
					resolvePropertyAccess( creationContext, property ),
					property.getValue() instanceof OneToOne || ( (ManyToOne) property.getValue() ).isLogicalOneToOne()
							? SingularAttributeClassification.ONE_TO_ONE
							: SingularAttributeClassification.MANY_TO_ONE,
					makeEntityType( creationContext, toOne ),
					Disposition.NORMAL,
					creationContext.getTypeConfiguration().findEntityPersister( toOne.getReferencedEntityName() ),
					columns
			);
		}
		else {
			assert property.getValue() instanceof SimpleValue;

			final SimpleValue simpleValue = (SimpleValue) property.getValue();

			final AttributeConverterDefinition attributeConverterInfo = simpleValue.getAttributeConverterDescriptor();

			return new SingularPersistentAttributeBasic<>(
					source,
					property.getName(),
					resolvePropertyAccess( creationContext, property ),
					resolveBasicType( creationContext, simpleValue ),
					Disposition.NORMAL,
					attributeConverterInfo,
					columns
			);

		}
	}

	@SuppressWarnings("unchecked")
	private <J> BasicType<J> resolveBasicType(PersisterCreationContext creationContext, SimpleValue simpleValue) {
		if ( simpleValue.getCurrentType() != null ) {
			return (BasicType<J>) simpleValue.getCurrentType();
		}

		return creationContext.getTypeConfiguration().getBasicTypeRegistry().resolveBasicType(
				simpleValue.getBasicTypeParameters(),
				simpleValue.makeJdbcRecommendedSqlTypeMappingContext( creationContext.getTypeConfiguration() )
		);
	}

	private EntityType makeEntityType(PersisterCreationContext creationContext, ToOne toOne) {
		final String referencedEntityName = toOne.getReferencedEntityName();
		final EntityPersister<?> entityPersister = creationContext.getTypeConfiguration().findEntityPersister( referencedEntityName );

		return new EntityTypeImpl(
				null,
				entityPersister.getJavaTypeDescriptor(),
				creationContext.getTypeConfiguration()
		);
	}

	private PropertyAccess resolvePropertyAccess(PersisterCreationContext persisterCreationContext, Property property) {
		final PropertyAccessStrategy strategy = property.getPropertyAccessStrategy( property.getPersistentClass().getMappedClass() );
		return strategy.buildPropertyAccess( property.getPersistentClass().getMappedClass(), property.getName() );
	}

	private static String extractEmbeddableName(Type attributeType) {
		// todo : fixme
		return attributeType.getName();
	}

	public PersistentAttribute buildPluralAttribute(
			PersisterCreationContext creationContext,
			ManagedTypeImplementor source,
			Property property) {
		final PersisterFactory persisterFactory = creationContext.getSessionFactory().getServiceRegistry().getService( PersisterFactory.class );

			// todo : resolve cache access
		final CollectionRegionAccessStrategy cachingAccess = null;

		// need PersisterCreationContext - we should always have access to that when building persisters, through finalized initialization
		final CollectionPersister collectionPersister = persisterFactory.createCollectionPersister(
				(Collection) property.getValue(),
				source,
				property.getName(),
				cachingAccess,
				creationContext
		);
		creationContext.registerCollectionPersister( collectionPersister );
		return collectionPersister;
	}

	public static PropertyAccess resolvePropertyAccess(IdentifiableTypeImplementor declarer, Property property) {
		final PropertyAccessStrategy strategy = property.getPropertyAccessStrategy( declarer.getJavaType() );
		return strategy.buildPropertyAccess( declarer.getJavaType(), property.getName() );
	}

	public static List<Column> makeValues(
			SessionFactoryImplementor sessionFactory,
			Type type,
			Iterator<Selectable> columnIterator,
			Table separateCollectionTable) {
		return null;
	}

	public interface CollectionMetadata {
		CollectionClassification getCollectionClassification();
		ElementClassification getElementClassification();

		Type getForeignKeyType();
		BasicType getCollectionIdType();
		Type getElementType();
		Type getIndexType();
	}

	public static class CollectionMetadataImpl implements CollectionMetadata {
		private final CollectionClassification collectionClassification;
		private final ElementClassification elementClassification;
		private final Type foreignKeyType;
		private final BasicType collectionIdType;
		private final Type elementType;
		private final Type indexType;

		public CollectionMetadataImpl(
				CollectionClassification collectionClassification,
				ElementClassification elementClassification,
				Type foreignKeyType,
				BasicType collectionIdType,
				Type elementType,
				Type indexType) {
			this.collectionClassification = collectionClassification;
			this.elementClassification = elementClassification;
			this.foreignKeyType = foreignKeyType;
			this.collectionIdType = collectionIdType;
			this.elementType = elementType;
			this.indexType = indexType;
		}

		@Override
		public CollectionClassification getCollectionClassification() {
			return collectionClassification;
		}

		@Override
		public ElementClassification getElementClassification() {
			return elementClassification;
		}

		@Override
		public Type getForeignKeyType() {
			return foreignKeyType;
		}

		@Override
		public BasicType getCollectionIdType() {
			return collectionIdType;
		}

		@Override
		public Type getElementType() {
			return elementType;
		}

		@Override
		public Type getIndexType() {
			return indexType;
		}
	}

	public static CollectionMetadata interpretCollectionMetadata(SessionFactoryImplementor factory, Property property) {
		final Collection collectionBinding = (Collection) property.getValue();

		return new CollectionMetadataImpl(
				interpretCollectionClassification( collectionBinding ),
				interpretElementClassification( collectionBinding ),
				collectionBinding.getKey().getType(),
				collectionBinding instanceof IdentifierCollection
						? (BasicType) ( (IdentifierCollection) collectionBinding ).getIdentifier().getType()
						: null,
				collectionBinding.getElement().getType(),
				( (IndexedCollection) collectionBinding ).getIndex().getType()
		);
	}

	public static CollectionClassification interpretCollectionClassification(Collection collectionBinding) {
		if ( collectionBinding instanceof Bag || collectionBinding instanceof IdentifierBag ) {
			return CollectionClassification.BAG;
		}
		else if ( collectionBinding instanceof org.hibernate.mapping.List ) {
			return CollectionClassification.LIST;
		}
		else if ( collectionBinding instanceof org.hibernate.mapping.Set ) {
			return CollectionClassification.SET;
		}
		else if ( collectionBinding instanceof org.hibernate.mapping.Map ) {
			return CollectionClassification.MAP;
		}
		else {
			final Class javaType = collectionBinding.getElement().getType().getJavaType();
			if ( Set.class.isAssignableFrom( javaType ) ) {
				return CollectionClassification.SET;
			}
			else if ( Map.class.isAssignableFrom( javaType ) ) {
				return CollectionClassification.MAP;
			}
			else if ( List.class.isAssignableFrom( javaType ) ) {
				return CollectionClassification.LIST;
			}

			return CollectionClassification.BAG;
		}
	}

	private static ElementClassification interpretElementClassification(Collection collectionBinding) {
		if ( collectionBinding.getElement() instanceof Any ) {
			return ElementClassification.ANY;
		}
		else if ( collectionBinding.getElement() instanceof Component ) {
			return ElementClassification.EMBEDDABLE;
		}
		else if ( collectionBinding.getElement() instanceof OneToMany ) {
			return ElementClassification.ONE_TO_MANY;
		}
		else if ( collectionBinding.getElement() instanceof ManyToOne ) {
			return ElementClassification.MANY_TO_MANY;
		}
		else {
			return ElementClassification.BASIC;
		}
	}


	public static SingularAttributeClassification interpretIdentifierClassification(Type ormIdType) {
		return ormIdType instanceof EmbeddedType
				? SingularAttributeClassification.EMBEDDED
				: SingularAttributeClassification.BASIC;
	}
}
