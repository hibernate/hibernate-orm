/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SortableValue;
import org.hibernate.mapping.Value;
import org.hibernate.type.ForeignKeyDirection;

import jakarta.persistence.ForeignKey;

import static org.hibernate.boot.model.internal.BinderHelper.checkMappedByType;
import static org.hibernate.boot.model.internal.BinderHelper.findPropertyByName;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * We have to handle {@link jakarta.persistence.OneToOne} associations
 * in a second pass.
 */
public class OneToOneSecondPass implements SecondPass {
	private final MetadataBuildingContext buildingContext;
	private final OneToOne oneToOne;
	private final PropertyBinder binder;
	private final Property property;
	private final String mappedBy;
	private final String ownerEntity;
	private final PropertyHolder propertyHolder;
	private final PropertyData inferredData;
	private final NotFoundAction notFoundAction;
	private final AnnotatedJoinColumns joinColumns;
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
		oneToOne.sortProperties();
	}

	private void bindUnowned(Map<String, PersistentClass> persistentClasses, OneToOne oneToOne) {
		oneToOne.setMappedByProperty( mappedBy );
		final String targetEntityName = oneToOne.getReferencedEntityName();
		final PersistentClass targetEntity = persistentClasses.get( targetEntityName );
		if ( targetEntity == null ) {
			final String problem = annotatedEntity
					? " which does not belong to the same persistence unit"
					: " which is not an '@Entity' type";
			throw new MappingException( "Association '" + getPath( propertyHolder, inferredData )
					+ "' targets the type '" + targetEntityName + "'" + problem );
		}
		final Property targetProperty = targetProperty( oneToOne, targetEntity );
		final Value targetPropertyValue = targetProperty.getValue();
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
				targetProperty.getValue(),
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
		for ( Join otherSideJoinValue : targetEntity.getJoins() ) {
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
			final ManyToOne manyToOne = new ManyToOne( buildingContext, mappedByJoin.getTable() );
			manyToOne.setNotFoundAction( notFoundAction );
			manyToOne.setOnDeleteAction( oneToOne.getOnDeleteAction() );
			manyToOne.setFetchMode( oneToOne.getFetchMode() );
			manyToOne.setLazy( oneToOne.isLazy() );
			manyToOne.setReferencedEntityName( oneToOne.getReferencedEntityName() );
			manyToOne.setReferencedPropertyName( mappedBy );
			manyToOne.setUnwrapProxy( oneToOne.isUnwrapProxy() );
			manyToOne.markAsLogicalOneToOne();
			property.setValue( manyToOne );
			for ( Column column: otherSideJoin.getKey().getColumns() ) {
				Column copy = column.clone();
				copy.setValue( manyToOne );
				manyToOne.addColumn( copy );
			}
			// The property was added to the propertyHolder eagerly to have knowledge about this property,
			// in order for de-duplication to kick in, but we move it to a join if necessary
			propertyHolder.movePropertyToJoin( property, mappedByJoin, inferredData.getDeclaringClass() );
		}

		oneToOne.setReferencedPropertyName( mappedBy );

		// HHH-6813
		// Foo: @Id long id, @OneToOne(mappedBy="foo") Bar bar
		// Bar: @Id @OneToOne Foo foo
		final KeyValue targetEntityIdentifier = targetEntity.getIdentifier();
		boolean referenceToPrimaryKey = mappedBy == null
				|| targetEntityIdentifier instanceof Component
						&& ( (Component) targetEntityIdentifier ).matchesAllProperties( mappedBy );
		oneToOne.setReferenceToPrimaryKey( referenceToPrimaryKey );

		final String propertyRef = oneToOne.getReferencedPropertyName();
		if ( propertyRef != null ) {
			buildingContext.getMetadataCollector()
					.addUniquePropertyReference( oneToOne.getReferencedEntityName(), propertyRef );
		}
	}

	private Property targetProperty(OneToOne oneToOne, PersistentClass targetEntity) {
		try {
			Property targetProperty = findPropertyByName( targetEntity, mappedBy );
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
		final ToOneFkSecondPass secondPass = new ToOneFkSecondPass(
				oneToOne,
				joinColumns,
				true,
				annotatedEntity,
				propertyHolder.getPersistentClass(),
				qualify( propertyHolder.getPath(), propertyName ),
				buildingContext
		);
		secondPass.doSecondPass(persistentClasses);
		//no column associated since it's a one to one
	}

	/**
	 * Builds the {@link Join} instance for the unowned side
	 * of a {@code OneToOne} association using a join table.
	 * From the {@code mappedBy} side we should not create
	 * neither the PK, nor the FK, this is all handled from
	 * the owning side.
	 */
	private Join buildJoinFromMappedBySide(PersistentClass persistentClass, Property otherSideProperty, Join originalJoin) {
		Join join = new Join();
		join.setPersistentClass( persistentClass );

		//no check constraints available on joins
		join.setTable( originalJoin.getTable() );
		join.setInverse( true );
		DependantValue key = new DependantValue( buildingContext, join.getTable(), persistentClass.getIdentifier() );

		if ( notFoundAction != null ) {
			join.disableForeignKeyCreation();
		}

		join.setKey( key );
		//perhaps not quite per-spec, but a Good Thing anyway
		join.setOptional( true );
		key.setOnDeleteAction( null );
		for ( Column column: otherSideProperty.getValue().getColumns() ) {
			Column copy = column.clone();
			copy.setValue( key );
			key.addColumn( copy );
		}
		if ( otherSideProperty.getValue() instanceof SortableValue ) {
			final SortableValue value = (SortableValue) otherSideProperty.getValue();
			if ( !value.isSorted() ) {
				key.sortProperties();
			}
		}
		persistentClass.addJoin( join );
		return join;
	}
}
