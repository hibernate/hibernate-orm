/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.HibernateException;
import org.hibernate.engine.FetchTiming;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Mapping of a discriminator, for either {@linkplain EntityMappingType#getDiscriminatorMapping() entity} or
 * {@linkplain DiscriminatedAssociationModelPart#getDiscriminatorMapping() association} (ANY) discrimination.
 * <p/>
 * Represents a composition of <ul>
 *     <li>a {@linkplain #getValueConverter() converter} between the domain and relational form</li>
 *     <li>a {@linkplain #getUnderlyingJdbcMapping JDBC mapping} to read and write the relational values</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface DiscriminatorMapping extends VirtualModelPart, BasicValuedModelPart, Fetchable {
	/**
	 * Information about the value mappings
	 */
	DiscriminatorConverter<?,?> getValueConverter();

	/**
	 * Retrieve the {@linkplain DiscriminatorValueDetails details} for a particular discriminator value.
	 *
	 * @throws HibernateException if there is value matching the provided one
	 */
	default DiscriminatorValueDetails resolveDiscriminatorValue(Object discriminatorValue) {
		return getValueConverter().getDetailsForDiscriminatorValue( discriminatorValue );
	}

	JdbcMapping getUnderlyingJdbcMapping();

	/**
	 * The domain Java form, which is either {@code JavaType<Class>} (entity class)
	 * or {@code JavaType<String>} (entity name).
	 */
	default JavaType<?> getDomainJavaType() {
		return getValueConverter().getDomainJavaType();
	}

	/**
	 * The relational Java form.  This will typically be some form of integer
	 * or character value.
	 */
	default JavaType<?> getRelationalJavaType() {
		return getValueConverter().getRelationalJavaType();
	}

	/**
	 * Create the appropriate SQL expression for this discriminator
	 *
	 * @param jdbcMappingToUse The JDBC mapping to use.  This allows opting between
	 * the "domain result type" (aka Class) and the "underlying type" (Integer, String, etc)
	 */
	Expression resolveSqlExpression(
			NavigablePath navigablePath,
			JdbcMapping jdbcMappingToUse,
			TableGroup tableGroup,
			SqlAstCreationState creationState);

	@Override
	BasicFetch<?> generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState);
}
