package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SortableValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;

import static org.hibernate.boot.model.internal.BinderHelper.checkMappedByType;
import static org.hibernate.boot.model.internal.BinderHelper.findPropertyByName;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.boot.model.internal.ForeignKeyType.NON_PRIMARY_KEY_REFERENCE;

/**
 * We have to handle {@link jakarta.persistence.OneToOne} associations
 * in a second pass.
 */
public class OneToOneSecondPass implements SecondPass {
	private final PropertyData inferredData;
	private final PropertyHolder propertyHolder;
	private final OneToOne oneToOne;
	private final PropertyBinder binder;
	private final Property property;
	private final String mappedBy;
	private final String ownerEntity;
	private final NotFoundAction notFoundAction;
	private final AnnotatedJoinColumns joinColumns;
	private final MetadataBuildingContext buildingContext;
	private final boolean annotatedEntity;

	public OneToOneSecondPass(
			PropertyBinder binder,
			Property property,
			OneToOne oneToOne,
			String mappedBy,
			String ownerEntity,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean annotatedEntity,
			NotFoundAction notFoundAction,
			AnnotatedJoinColumns columns,
			MetadataBuildingContext buildingContext) {
		this.binder = binder;
		this.property = property;
		this.oneToOne = oneToOne;
		this.ownerEntity = ownerEntity;
		this.mappedBy = mappedBy;
		this.propertyHolder = propertyHolder;
		this.buildingContext = buildingContext;
		this.notFoundAction = notFoundAction;
		this.inferredData = inferredData;
		this.annotatedEntity = annotatedEntity;
		this.joinColumns = columns;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		if ( mappedBy == null ) {
			bindOwned( persistentClasses, oneToOne, inferredData.getPropertyName() );
		}
		else {
			bindUnowned( persistentClasses, oneToOne );
		}
		binder.callAttributeBindersInSecondPass( property );
		((SortableValue) property.getValue()).sortProperties();
	}

	private void bindUnowned(Map<String, PersistentClass> persistentClasses, OneToOne oneToOne) {
		oneToOne.setMappedByProperty( mappedBy );
		final String targetEntityName = oneToOne.getReferencedEntityName();
		final var targetEntity = persistentClasses.get( targetEntityName );
		if ( targetEntity == null ) {
			final String problem = annotatedEntity
					? " which does not belong to the same persistence unit"
					: " which is not an '@Entity' type";
			throw new MappingException( "Association '" + getPath( propertyHolder, inferredData )
					+ "' targets the type '" + targetEntityName + "'" + problem );
		}
		final var targetProperty = targetProperty( oneToOne, targetEntity );
		final var targetPropertyValue = targetProperty.getValue();
		if ( targetPropertyValue instanceof ManyToOne ) {
			bindTargetManyToOne( persistentClasses, oneToOne, targetEntity, targetProperty );
		}
		else if ( !(targetPropertyValue instanceof OneToOne) ) {
			throw new AnnotationException( "Association '" + getPath( propertyHolder, inferredData )
					+ "' is 'mappedBy' a property named '" + mappedBy
					+ "' of the target entity type '" + targetEntityName
					+ "' which is not a '@OneToOne' or '@ManyToOne' association" );
		}
		checkMappedByType(
				mappedBy,
				targetPropertyValue,
				oneToOne.getPropertyName(),
				propertyHolder,
				persistentClasses
		);
	}

	private void bindTargetManyToOne(
			Map<String, PersistentClass> persistentClasses,
			OneToOne oneToOne,
			PersistentClass targetEntity,
			Property targetProperty) {
		Join otherSideJoin = null;
		for ( var otherSideJoinValue : targetEntity.getJoins() ) {
			if ( otherSideJoinValue.containsProperty(targetProperty) ) {
				otherSideJoin = otherSideJoinValue;
				break;
			}
		}
		if ( otherSideJoin != null ) {
			//@OneToOne @JoinTable
			final Join mappedByJoin = buildJoinFromMappedBySide(
					persistentClasses.get( ownerEntity ), targetProperty, otherSideJoin
			);
			final var manyToOne = createManyToOne( oneToOne, mappedByJoin.getTable() );
			property.setValue( manyToOne );
			for ( var column: otherSideJoin.getKey().getColumns() ) {
				final var copy = column.clone();
				copy.setValue( manyToOne );
				manyToOne.addColumn( copy );
			}
			// The property was added to the propertyHolder eagerly to have knowledge about this property,
			// in order for de-duplication to kick in, but we move it to a join if necessary
			propertyHolder.movePropertyToJoin( property, mappedByJoin, inferredData.getAttributeMember(), inferredData.getDeclaringClass() );
		}

		oneToOne.setReferencedPropertyName( mappedBy );

		// HHH-6813
		// Foo: @Id long id, @OneToOne(mappedBy="foo") Bar bar
		// Bar: @Id @OneToOne Foo foo
		final boolean referenceToPrimaryKey = mappedBy == null
				|| targetEntity.getIdentifier() instanceof Component compositeId
						&& compositeId.matchesAllProperties( mappedBy );
		oneToOne.setReferenceToPrimaryKey( referenceToPrimaryKey );

		final String propertyRef = oneToOne.getReferencedPropertyName();
		if ( propertyRef != null ) {
			buildingContext.getMetadataCollector()
					.addUniquePropertyReference( oneToOne.getReferencedEntityName(), propertyRef );
		}
	}

	private ManyToOne createManyToOne(OneToOne oneToOne, Table table) {
		final ManyToOne manyToOne = new ManyToOne( buildingContext, table );
		manyToOne.setNotFoundAction( notFoundAction );
		manyToOne.copyRestrictions( oneToOne );
		manyToOne.setOnDeleteAction( oneToOne.getOnDeleteAction() );
		manyToOne.setFetchStyle( oneToOne.getFetchStyle() );
		manyToOne.setLazy( oneToOne.isLazy() );
		manyToOne.setReferencedEntityName( oneToOne.getReferencedEntityName() );
		manyToOne.setReferencedPropertyName( mappedBy );
		manyToOne.setUnwrapProxy( oneToOne.isUnwrapProxy() );
		manyToOne.markAsLogicalOneToOne();
		return manyToOne;
	}

	private Property targetProperty(OneToOne oneToOne, PersistentClass targetEntity) {
		try {
			final var targetProperty = findPropertyByName( targetEntity, mappedBy );
			if ( targetProperty != null ) {
				return targetProperty;
			}
		}
		catch (MappingException e) {
			// swallow it
		}
		throw new AnnotationException( "Association '" + getPath( propertyHolder, inferredData )
				+ "' is 'mappedBy' a property named '" + mappedBy
				+ "' which does not exist in the target entity type '" + oneToOne.getReferencedEntityName() + "'" );
	}

	private void bindOwned(
			Map<String, PersistentClass> persistentClasses,
			OneToOne oneToOne,
			String propertyName) {
		ToOne value = oneToOne;
		if ( ToOneBinder.isFormulaOneToOne( joinColumns )
				&& !bindFormulaSelectables( persistentClasses, oneToOne, propertyName ) ) {
			// Preserve the logical many-to-one path for references to a non-primary key.
			value = createManyToOne( oneToOne, oneToOne.getTable() );
			value.setUnwrapProxyImplicit( oneToOne.isUnwrapProxyImplicit() );
			property.setValue( value );
			binder.setValue( value );
		}
		final ToOneFkSecondPass secondPass = new ToOneFkSecondPass(
				value,
				joinColumns,
				true,
				annotatedEntity,
				propertyHolder.getPersistentClass(),
				qualify( propertyHolder.getPath(), propertyName ),
				buildingContext
		);
		secondPass.doSecondPass(persistentClasses);
	}

	/**
	 * Attaches the explicit formula and read-only column selectables of a formula-based one-to-one
	 * (see {@link ToOneBinder#isFormulaOneToOne}) onto the {@code OneToOne} mapping
	 * value, in the order of the target entity's identifier. This is what makes
	 * {@link OneToOne#getType()} select {@code SpecialOneToOneType}, matching the
	 * legacy {@code hbm.xml} join-formula one-to-one.
	 *
	 * @return false if the reference must retain logical many-to-one binding
	 */
	private boolean bindFormulaSelectables(
			Map<String, PersistentClass> persistentClasses,
			OneToOne oneToOne,
			String propertyName) {
		final String path = qualify( propertyHolder.getPath(), propertyName );
		final String targetEntityName = oneToOne.getReferencedEntityName();
		final var targetEntity = persistentClasses.get( targetEntityName );
		if ( targetEntity == null ) {
			final String problem = annotatedEntity
					? " which does not belong to the same persistence unit"
					: " which is not an '@Entity' type";
			throw new MappingException( "Association '" + path
					+ "' targets the type '" + targetEntityName + "'" + problem );
		}

		if ( joinColumns.getReferencedColumnsType( targetEntity ) == NON_PRIMARY_KEY_REFERENCE ) {
			return false;
		}

		final var columns = joinColumns.getJoinColumns();
		final var identifierColumns = targetEntity.getIdentifier().getColumns();
		if ( identifierColumns.size() != columns.size() ) {
			throw new AnnotationException( "Association '" + path
					+ "' has " + columns.size() + " join selectable(s) but the identifier of '" + targetEntityName
					+ "' has " + identifierColumns.size() + " column(s)" );
		}

		final long namedCount = columns.stream().filter( column -> !column.isReferenceImplicit() ).count();
		if ( namedCount > 0 && namedCount < columns.size() ) {
			throw new AnnotationException( "Association '" + path
					+ "' may not mix join selectables with and without an explicit 'referencedColumnName'" );
		}

		if ( namedCount == 0 ) {
			// positional resolution: declaration order is reconciled with the
			// target identifier's resolved order by the subsequent sortProperties() call
			for ( var column : columns ) {
				column.linkWithValue( oneToOne );
			}
		}
		else {
			if ( targetEntity.getIdentifier() instanceof SortableValue identifier ) {
				identifier.sortProperties();
			}
			final var metadataCollector = buildingContext.getMetadataCollector();
			for ( var idColumn : targetEntity.getIdentifier().getColumns() ) {
				final var matching = columns.stream()
						.filter( column -> idColumn.equals( new Column( metadataCollector.getPhysicalColumnName(
								targetEntity.getTable(), column.getReferencedColumn() ) ) ) )
						.findFirst()
						.orElseThrow( () -> new AnnotationException( "Association '" + path
								+ "' has no join selectable referencing identifier column '"
								+ idColumn.getName() + "' of '" + targetEntityName + "'" ) );
				matching.linkWithValue( oneToOne );
			}
			// the selectables are already attached in the target identifier's order
			oneToOne.setSorted( true );
		}
		return true;
	}

	/**
	 * Builds the {@link Join} instance for the unowned side
	 * of a {@code OneToOne} association using a join table.
	 * From the {@code mappedBy} side we should not create
	 * neither the PK, nor the FK, this is all handled from
	 * the owning side.
	 */
	private Join buildJoinFromMappedBySide(PersistentClass persistentClass, Property otherSideProperty, Join originalJoin) {
		final var join = new Join();
		join.setPersistentClass( persistentClass );

		//no check constraints available on joins
		join.setTable( originalJoin.getTable() );
		join.setInverse( true );
		final var key = new DependantValue( buildingContext, join.getTable(), persistentClass.getIdentifier() );

		if ( notFoundAction != null ) {
			join.disableForeignKeyCreation();
		}

		join.setKey( key );
		//perhaps not quite per-spec, but a Good Thing anyway
		join.setOptional( true );
		key.setOnDeleteAction( null );
		for ( var column: otherSideProperty.getValue().getColumns() ) {
			final var copy = column.clone();
			copy.setValue( key );
			key.addColumn( copy );
		}
		if ( otherSideProperty.getValue() instanceof SortableValue value
				&& !value.isSorted() ) {
			key.sortProperties();
		}
		persistentClass.addJoin( join );
		return join;
	}
}
