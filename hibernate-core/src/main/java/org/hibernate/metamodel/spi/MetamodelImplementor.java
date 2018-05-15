/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.spi;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.persistence.EntityGraph;

import org.hibernate.EntityNameResolver;
import org.hibernate.Metamodel;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * An SPI extension to the JPA {@link javax.persistence.metamodel.Metamodel}
 * via ({@link org.hibernate.Metamodel}
 *
 * @apiNote Most of that functionality has been moved to {@link TypeConfiguration} instead,
 * accessible via {@link #getTypeConfiguration()}
 */
public interface MetamodelImplementor extends Metamodel {
	// todo (6.0) : Should we move #getTypeConfiguration() from Metamodel to here?
	//  		In 5.x we expose #getTypeConfiguration() here but in 6.0 its on the api.

	// todo (6.0) : would be awesome to expose the runtime database model here
	//		however that has some drawbacks that we need to discuss, namely
	//		that DatabaseModel holds state that we do not need beyond
	//		schema-management tooling - init-commands and aux-db-objects


	Set<PersistentCollectionDescriptor<?,?,?>> findCollectionsByEntityParticipant(EntityTypeDescriptor entityDescriptor);

	Set<String> findCollectionRolesByEntityParticipant(EntityTypeDescriptor entityDescriptor);

	void visitEntityNameResolvers(Consumer<EntityNameResolver> action);

	/**
	 * When a Class is referenced in a query, this method is invoked to resolve
	 * its set of valid "implementors" as a group.  The returned expressable type
	 * encapsulates all known implementors
	 */
	<T> EntityValuedExpressableType<T> resolveEntityReference(Class<T> javaType);

	EntityValuedExpressableType resolveEntityReference(String entityName);

	AllowableParameterType resolveAllowableParamterType(Class clazz);

	<T> void addNamedEntityGraph(String graphName, RootGraphImplementor<T> entityGraph);

	<T> RootGraphImplementor<T> findEntityGraphByName(String name);

	<T> List<RootGraphImplementor<? super T>> findEntityGraphsByJavaType(Class<T> entityClass);

	/**
	 * @deprecated Use {@link #addNamedEntityGraph(String, RootGraphImplementor)} instead.
	 */
	@Deprecated
	<T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph);

	/**
	 * @deprecated Use {@link #findEntityGraphsByJavaType(Class)} instead.
	 */
	@Deprecated
	default <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
		return (List) findEntityGraphsByJavaType( entityClass );
	}

	@Override
	default <T> RootGraphImplementor<T> findRootGraph(String name) {
		return findEntityGraphByName( name );
	}

	/**
	 * Close the Metamodel
	 */
	void close();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Co-variant returns


	@Override
	<X> EntityTypeDescriptor<X> entity(String entityName);

	@Override
	<X> EntityTypeDescriptor<X> entity(Class<X> cls);

	@Override
	<X> ManagedTypeDescriptor<X> managedType(Class<X> cls);

	@Override
	<X> EmbeddedTypeDescriptor<X> embeddable(Class<X> cls);
}
