/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;
import java.util.Objects;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * A mapping for a one-to-many association
 *
 * @author Gavin King
 */
public class OneToMany implements Value {
	private final MetadataImplementor metadata;
	private final Table referencingTable;

	private String referencedEntityName;
	private PersistentClass associatedClass;
	private boolean ignoreNotFound;

	/**
	 * @deprecated Use {@link OneToMany#OneToMany(MetadataBuildingContext, PersistentClass)} instead.
	 */
	@Deprecated
	public OneToMany(MetadataImplementor metadata, PersistentClass owner) throws MappingException {
		this.metadata = metadata;
		this.referencingTable = ( owner == null ) ? null : owner.getTable();
	}

	public OneToMany(MetadataBuildingContext buildingContext, PersistentClass owner) throws MappingException {
		this.metadata = buildingContext.getMetadataCollector();
		this.referencingTable = ( owner == null ) ? null : owner.getTable();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return metadata.getMetadataBuildingOptions().getServiceRegistry();
	}

	private EntityType getEntityType() {
		return metadata.getTypeResolver().getTypeFactory().manyToOne(
				getReferencedEntityName(),
				true,
				null,
				false,
				false,
				isIgnoreNotFound(),
				false
		);
	}

	public PersistentClass getAssociatedClass() {
		return associatedClass;
	}

	/**
	 * Associated entity on the many side
	 */
	public void setAssociatedClass(PersistentClass associatedClass) {
		this.associatedClass = associatedClass;
	}

	@Override
	public void createForeignKey() {
		// no foreign key element of for a one-to-many
	}

	@Override
	public Iterator<Selectable> getColumnIterator() {
		return associatedClass.getKey().getColumnIterator();
	}

	@Override
	public int getColumnSpan() {
		return associatedClass.getKey().getColumnSpan();
	}

	@Override
	public FetchMode getFetchMode() {
		return FetchMode.JOIN;
	}

	/**
	 * Table of the owner entity (the "one" side)
	 */
	@Override
	public Table getTable() {
		return referencingTable;
	}

	@Override
	public Type getType() {
		return getEntityType();
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean isSimpleValue() {
		return false;
	}

	@Override
	public boolean isAlternateUniqueKey() {
		return false;
	}

	@Override
	public boolean hasFormula() {
		return false;
	}

	@Override
	public boolean isValid(Mapping mapping) throws MappingException {
		if ( referencedEntityName == null ) {
			throw new MappingException( "one to many association must specify the referenced entity" );
		}
		return true;
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	/**
	 * Associated entity on the "many" side
	 */
	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName == null ? null : referencedEntityName.intern();
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) {
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept( this );
	}

	@Override
	public boolean isSame(Value other) {
		return this == other || other instanceof OneToMany && isSame( (OneToMany) other );
	}

	public boolean isSame(OneToMany other) {
		return Objects.equals( referencingTable, other.referencingTable )
				&& Objects.equals( referencedEntityName, other.referencedEntityName )
				&& Objects.equals( associatedClass, other.associatedClass );
	}

	@Override
	public boolean[] getColumnInsertability() {
		//TODO: we could just return all false...
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean[] getColumnUpdateability() {
		//TODO: we could just return all false...
		throw new UnsupportedOperationException();
	}

	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}

}
