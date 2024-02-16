/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.internal.AnnotatedJoinColumn;
import org.hibernate.boot.model.internal.AnnotatedJoinColumns;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.EntityType;

import java.util.Objects;

import static org.hibernate.boot.model.internal.BinderHelper.findReferencedColumnOwner;

/**
 * A mapping model object representing an association where the target side has cardinality one.
 *
 * @author Gavin King
 */
public abstract class ToOne extends SimpleValue implements Fetchable, SortableValue {
	private FetchMode fetchMode;
	protected String referencedPropertyName;
	private String referencedEntityName;
	private String propertyName;
	private boolean lazy = true;
	private boolean sorted;
	private boolean unwrapProxy;
	private boolean unwrapProxyImplicit;
	private boolean referenceToPrimaryKey = true;

	protected ToOne(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );
	}

	protected ToOne(ToOne original) {
		super( original );
		this.fetchMode = original.fetchMode;
		this.referencedPropertyName = original.referencedPropertyName;
		this.referencedEntityName = original.referencedEntityName;
		this.propertyName = original.propertyName;
		this.lazy = original.lazy;
		this.sorted = original.sorted;
		this.unwrapProxy = original.unwrapProxy;
		this.unwrapProxyImplicit = original.unwrapProxyImplicit;
		this.referenceToPrimaryKey = original.referenceToPrimaryKey;
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode=fetchMode;
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public void setReferencedPropertyName(String name) {
		referencedPropertyName = name==null ? null : name.intern();
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName==null ?
				null : referencedEntityName.intern();
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName==null ?
				null : propertyName.intern();
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		if ( referencedEntityName == null ) {
			final ClassLoaderService cls = getMetadata().getMetadataBuildingOptions()
					.getServiceRegistry()
					.requireService( ClassLoaderService.class );
			referencedEntityName = ReflectHelper.reflectedPropertyClass( className, propertyName, cls ).getName();
		}
	}

	public boolean isTypeSpecified() {
		return referencedEntityName!=null;
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof ToOne && isSame( (ToOne) other );
	}

	public boolean isSame(ToOne other) {
		return super.isSame( other )
			&& Objects.equals( referencedPropertyName, other.referencedPropertyName )
			&& Objects.equals( referencedEntityName, other.referencedEntityName );
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		if (referencedEntityName==null) {
			throw new MappingException("association must specify the referenced entity");
		}
		return super.isValid( mapping );
	}

	public boolean isLazy() {
		return lazy;
	}
	
	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public boolean isUnwrapProxy() {
		return unwrapProxy;
	}

	public void setUnwrapProxy(boolean unwrapProxy) {
		this.unwrapProxy = unwrapProxy;
	}

	public boolean isUnwrapProxyImplicit() {
		return unwrapProxyImplicit;
	}

	/**
	 * Related to HHH-13658 - keep track of whether `unwrapProxy` is an implicit value
	 * for reference later
	 */
	public void setUnwrapProxyImplicit(boolean unwrapProxyImplicit) {
		this.unwrapProxyImplicit = unwrapProxyImplicit;
	}

	public boolean isReferenceToPrimaryKey() {
		return referenceToPrimaryKey;
	}

	public void setReferenceToPrimaryKey(boolean referenceToPrimaryKey) {
		this.referenceToPrimaryKey = referenceToPrimaryKey;
	}

	@Override
	public boolean isSorted() {
		return sorted;
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	@Override
	public int[] sortProperties() {
		final PersistentClass entityBinding = getMetadata().getEntityBinding( referencedEntityName );
		if ( entityBinding != null ) {
			final Value value = referencedPropertyName == null
					? entityBinding.getIdentifier()
					: entityBinding.getRecursiveProperty( referencedPropertyName ).getValue();
			if ( value instanceof Component ) {
				final Component component = (Component) value;
				final int[] originalPropertyOrder = component.sortProperties();
				if ( !sorted ) {
					if ( originalPropertyOrder != null ) {
						sortColumns( originalPropertyOrder );
					}
					sorted = true;
				}
				return originalPropertyOrder;
			}
			else {
				sorted = true;
			}
		}
		return null;
	}

	@Override
	public void createForeignKey(PersistentClass referencedEntity, AnnotatedJoinColumns joinColumns) {
		// Ensure properties are sorted before we create a foreign key
		sortProperties();
		if ( isForeignKeyEnabled() && referencedPropertyName==null && !hasFormula() ) {
			if ( isConstrained() ) {
				final AnnotatedJoinColumn firstColumn = joinColumns.getJoinColumns().get(0);
				final Object owner = findReferencedColumnOwner( referencedEntity, firstColumn, getBuildingContext() );
				if ( owner instanceof Join ) {
					// Here we handle the case of a foreign key that refers to the
					// primary key of a secondary table of the referenced entity
					final Join join = (Join) owner;
					final ForeignKey foreignKey = getTable().createForeignKey(
							getForeignKeyName(),
							getConstraintColumns(),
							referencedEntity.getEntityName(),
							getForeignKeyDefinition(),
							join.getKey().getColumns()
					);
					foreignKey.setOnDeleteAction( getOnDeleteAction() );
					foreignKey.setReferencedTable( join.getTable() );
				}
				else {
					// it's just a reference to the primary key of the main table
					createForeignKeyOfEntity( referencedEntity.getEntityName() );
				}
			}
		}
	}

	@Override
	public void createForeignKey() {
		// Ensure properties are sorted before we create a foreign key
		sortProperties();
		// A non-null referencedPropertyName tells us that the foreign key
		// does not reference the primary key, but some other unique key of
		// the referenced table. We do not handle this case here:
		// - For ManyToOne, the case of a foreign key to something other than
		//   the primary key is handled in createPropertyRefConstraints()
		// - For OneToOne, we still need to add some similar logic somewhere
		//   (for now, no foreign key constraint is created)
		if ( isForeignKeyEnabled() && referencedPropertyName==null && !hasFormula() ) {
			createForeignKeyOfEntity( ( (EntityType) getType() ).getAssociatedEntityName() );
		}
	}
}
