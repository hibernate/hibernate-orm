/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * A mapping model object representing a {@linkplain jakarta.persistence.ManyToOne many-to-one association}.
 *
 * @author Gavin King
 */
public class ManyToOne extends ToOne {
	private boolean isLogicalOneToOne;
	private NotFoundAction notFoundAction;

	private Type resolvedType;

	public ManyToOne(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );
	}

	private ManyToOne(ManyToOne original) {
		super( original );
		this.notFoundAction = original.notFoundAction;
		this.isLogicalOneToOne = original.isLogicalOneToOne;
	}

	@Override
	public ManyToOne copy() {
		return new ManyToOne( this );
	}

	public Type getType() throws MappingException {
		if ( resolvedType == null ) {
			resolvedType = MappingHelper.manyToOne(
					getReferencedEntityName(),
					isReferenceToPrimaryKey(),
					getReferencedPropertyName(),
					getPropertyName(),
					isLogicalOneToOne(),
					isLazy(),
					isUnwrapProxy(),
					isIgnoreNotFound(),
					getBuildingContext()
			);
		}

		return resolvedType;
	}

	@Override
	public void createUniqueKey(MetadataBuildingContext context) {
		if ( !hasFormula() ) {
			getTable().createUniqueKey( getConstraintColumns(), context );
		}
	}

	/**
	 * Creates a {@linkplain ForeignKey foreign key constraint} in the
	 * case that the foreign key of this association does not reference
	 * the primary key of the referenced table, but instead some other
	 * unique key.
	 * <p>
	 * We depend here on having a property of the referenced entity
	 * that does hold the referenced unique key. We might have created
	 * a "synthetic" composite property for this purpose.
	 */
	public void createPropertyRefConstraints(Map<String, PersistentClass> persistentClasses) {
		if ( referencedPropertyName != null ) {
			// Ensure properties are sorted before we create a foreign key
			sortProperties();

			final String referencedEntityName = getReferencedEntityName();
			final String referencedPropertyName = getReferencedPropertyName();
			final PersistentClass referencedClass = persistentClasses.get( referencedEntityName );
			if ( referencedClass == null ) {
				throw new MappingException( "Referenced entity '" + referencedEntityName + "' does not exist" );

			}
			final Property property = referencedClass.getReferencedProperty( referencedPropertyName );
			if ( property==null ) {
				throw new MappingException( "Referenced entity '" + referencedEntityName
						+ "' has no property named '" + referencedPropertyName + "'" );
			} 
			else {
				// Make sure synthetic properties are sorted
				if ( property.getValue() instanceof Component ) {
					( (Component) property.getValue() ).sortProperties();
				}
				// todo : if "none" another option is to create the ForeignKey object still	but to set its #disableCreation flag
				if ( isForeignKeyEnabled() && !hasFormula() ) {
					final ForeignKey foreignKey = getTable().createForeignKey(
							getForeignKeyName(), 
							getConstraintColumns(), 
							( (EntityType) getType() ).getAssociatedEntityName(), 
							getForeignKeyDefinition(),
							new ArrayList<>( property.getColumns() )
					);
					foreignKey.setReferencedTable( property.getValue().getTable() );
					foreignKey.setOnDeleteAction( getOnDeleteAction() );
				}
			}
		}
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public NotFoundAction getNotFoundAction() {
		return notFoundAction;
	}

	public void setNotFoundAction(NotFoundAction notFoundAction) {
		this.notFoundAction = notFoundAction;
	}

	public boolean isIgnoreNotFound() {
		return notFoundAction == NotFoundAction.IGNORE;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.notFoundAction = ignoreNotFound
				? NotFoundAction.IGNORE
				: null;
	}

	public void markAsLogicalOneToOne() {
		this.isLogicalOneToOne = true;
	}

	public boolean isLogicalOneToOne() {
		return isLogicalOneToOne;
	}

	@Override
	public boolean isNullable() {
		return getReferencedPropertyName() != null || super.isNullable();
	}
}
