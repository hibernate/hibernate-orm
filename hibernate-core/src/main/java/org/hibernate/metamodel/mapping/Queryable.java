/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * Defines a mapping model contract for things that can be queried in the HQL,
 * Criteria, etc sense.  Generally this
 *
 * todo (6.0) : consider whether collections are Queryable
 * 		- depends how we envision Queryable being used.  E.g. does it make
 * 		sense to allow calls like `findSubPart( "index" )` or `findSubPart( "element" )`?
 *
 * @author Steve Ebersole
 */
public interface Queryable extends ModelPart {
	/**
	 * For an entity, this form allows for Hibernate's "implicit treat" support -
	 * meaning it should find a sub-part whether defined on the entity, its
	 * super-type or even one of its sub-types.
	 *
	 * @implNote Logically the implementation should consider
	 * {@link org.hibernate.jpa.spi.JpaCompliance}.  Not passed in because it
	 * is expected that implementors have access to the SessionFactory to access
	 * the JpaCompliance.  See {@link SessionFactoryOptions#getJpaCompliance}
	 */
	ModelPart findSubPart(String name, EntityMappingType treatTargetType);

	/**
	 * Like {@link #findSubPart}, this form visits all parts defined on the
	 * entity, its super-types and its sub-types.
	 */
	void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType);

	/**
	 * Perform any preparation for using `this` as the left-hand side for the given `subPartSqmPath`.
	 *
	 * @param navigablePath The right-hand side NavigablePath.  The path's parent should reference this mapping type.
	 */
	TableGroup prepareAsLhs(NavigablePath navigablePath, SqlAstCreationState creationState);
}
