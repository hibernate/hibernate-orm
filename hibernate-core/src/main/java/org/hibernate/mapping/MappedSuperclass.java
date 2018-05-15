/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.metamodel.Type;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.MappedJoin;
import org.hibernate.boot.model.domain.MappedSuperclassJavaTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.internal.AbstractMappedSuperclassMapping;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.model.domain.RepresentationMode;

/**
 * Represents a @MappedSuperclass.
 * A @MappedSuperclass can be a superclass of an @Entity (root or not)
 *
 * This class primary goal is to give a representation to @MappedSuperclass
 * in the metamodel in order to reflect them in the JPA 2 metamodel.
 *
 * Do not use outside this use case.
 *
 * 
 * A proper redesign will be evaluated in Hibernate 4
 *
 * Implementation details:
 * properties are copies of their closest sub-persistentClass versions
 *
 * @author Emmanuel Bernard
 */
public class MappedSuperclass extends AbstractMappedSuperclassMapping implements PropertyContainer {
	public MappedSuperclass(
			EntityMappingHierarchy entityMappingHierarchy,
			IdentifiableTypeMapping superIdentifiableTypeMapping,
			MappedSuperclassJavaTypeMapping javaTypeMapping) {
		super( entityMappingHierarchy );
		setJavaTypeMapping( javaTypeMapping );
		setSuperManagedType( superIdentifiableTypeMapping );
	}

	@Override
	public RepresentationMode getExplicitRepresentationMode() {
		return getEntityMappingHierarchy().getExplicitRepresentationMode();
	}

	/**
	 * Returns the first superclass marked as @MappedSuperclass or null if:
	 *  - none exists
	 *  - or the first persistent superclass found is an @Entity
	 *
	 * @return the super MappedSuperclass
	 */
	public MappedSuperclass getSuperMappedSuperclass() {
		return (MappedSuperclass) getSuperManagedTypeMappingOfType( Type.PersistenceType.MAPPED_SUPERCLASS );
	}

	/**
	 * @deprecated since 6.0, use {@link #hasSingleIdentifierAttributeMapping()}.
	 */
	@Deprecated
	public boolean hasIdentifierProperty() {
		return hasSingleIdentifierAttributeMapping();
	}

	/**
	 * @deprecated since 6.0, use {@link #hasVersionAttributeMapping()}.
	 */
	@Deprecated
	public boolean isVersioned() {
		return hasVersionAttributeMapping();
	}

	/**
	 * Returns the PersistentClass of the first superclass marked as @Entity
	 * or null if none exists
	 *
	 * @return the PersistentClass of the superclass
	 */
	public PersistentClass getSuperPersistentClass() {
		return (PersistentClass) getSuperManagedTypeMappingOfType( Type.PersistenceType.ENTITY );
	}

	/**
	 * @deprecated since 6.0, use {@link #getDeclaredPersistentAttributes()}.
	 */
	@Deprecated
	public Iterator getDeclaredPropertyIterator() {
		return getDeclaredProperties().iterator();
	}

	/**
	 * @param p the declared property.
	 * @deprecated since 6.0, use {@link #addDeclaredPersistentAttribute(PersistentAttributeMapping)}.
	 */
	@Deprecated
	public void addDeclaredProperty(Property p) {
		addDeclaredPersistentAttribute( p );
	}

	/**
	 * @deprecated since 6.0, use {@link #getIdentifierAttributeMapping()}.
	 */
	@Deprecated
	public Property getIdentifierProperty() {
		return (Property) getIdentifierAttributeMapping();
	}

	/**
	 * @deprecated since 6.0, use {@link #getDeclaredIdentifierAttributeMapping()}.
	 */
	@Deprecated
	public Property getDeclaredIdentifierProperty() {
		return (Property) getDeclaredIdentifierAttributeMapping();
	}

	/**
	 * @deprecated since 6.0, use {@link #setDeclaredIdentifierAttributeMapping(PersistentAttributeMapping)}.
	 */
	@Deprecated
	public void setDeclaredIdentifierProperty(Property prop) {
		setDeclaredIdentifierAttributeMapping( prop );
	}

	/**
	 * @deprecated since 6.0, use {@link #getVersionAttributeMapping()}.
	 */
	@Deprecated
	public Property getVersion() {
		return (Property) getVersionAttributeMapping();
	}

	/**
	 * @deprecated since 6.0, use {@link #getDeclaredVersionAttributeMapping()}.
	 */
	@Deprecated
	public Property getDeclaredVersion() {
		return (Property) getDeclaredVersionAttributeMapping();
	}

	/**
	 * @deprecated since 6.0, use {@link #setDeclaredVersionAttributeMapping(PersistentAttributeMapping)}.
	 */
	@Deprecated
	public void setDeclaredVersion(Property prop) {
		setDeclaredVersionAttributeMapping( prop );
	}

	/**
	 * @deprecated since 6.0, use {@link #getEmbeddedIdentifierAttributeMapping()}.
	 */
	@Deprecated
	public Component getIdentifierMapper() {
		return (Component) getEmbeddedIdentifierAttributeMapping();
	}

	/**
	 * @deprecated since 6.0, use {@link #getDeclaredEmbeddedIdentifierAttributeMapping()}.
	 */
	@Deprecated
	public Component getDeclaredIdentifierMapper() {
		return (Component) getDeclaredEmbeddedIdentifierAttributeMapping();
	}

	/**
	 * @deprecated since 6.0, use {@link #setDeclaredIdentifierEmbeddedValueMapping(EmbeddedValueMapping)}.
	 */
	@Deprecated
	public void setDeclaredIdentifierMapper(EmbeddedValueMapping identifierMapper) {
		setDeclaredIdentifierEmbeddedValueMapping( identifierMapper );
	}

	/**
	 * Check to see if this MappedSuperclass defines a property with the given name.
	 *
	 * @param name The property name to check
	 *
	 * @return {@code true} if a property with that name exists; {@code false} if not
	 *
	 * @deprecated since 6.0, use {@link #hasDeclaredPersistentAttribute(String)}.
	 */
	@Deprecated
	public boolean hasProperty(String name) {
		return hasDeclaredPersistentAttribute( name );
	}

	/**
	 * Check to see if a property with the given name exists in this MappedSuperclass
	 * or in any of its super hierarchy.
	 *
	 * @param name The property name to check
	 *
	 * @return {@code true} if a property with that name exists; {@code false} if not
	 * @deprecated since 6.0, use {@link #hasPersistentAttribute(String)}.
	 */
	@Deprecated
	public boolean isPropertyDefinedInHierarchy(String name) {
		return hasPersistentAttribute( name );
	}

	/**
	 * @deprecated since 6.0, use {@link #getSuperManagedTypeMapping()}.
	 */
	@Override
	@Deprecated
	public PropertyContainer getSuperPropertyContainer() {
		MappedSuperclass superMappedSuperclass = getSuperMappedSuperclass();
		if ( superMappedSuperclass != null ) {
			return superMappedSuperclass;
		}
		return getSuperPersistentClass();
	}

	/**
	 * @deprecated since 6.0, use {@link #getDeclaredPersistentAttributes()}.
	 */
	@Override
	@Deprecated
	public List<Property> getDeclaredProperties() {
		return getDeclaredPersistentAttributes().stream().map( e -> (Property) e ).collect( Collectors.toList() );
	}

	@Override
	public int nextSubclassId() {
		throw new MappingException( "This should not be called on a MappedSuperclass" );
	}

	@Override
	public Collection<MappedJoin> getMappedJoins() {
		throw new NotYetImplementedException( "Mapped superclass secondary tables is not implemented yet" );
	}

	@Override
	public MappedTable getMappedTable() {
		throw new MappingException( "This should not be called on a MappedSuperclass" );
	}
}
