/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;

/**
 * Descriptor for an entity's identifier
 *
 * @todo (6.0) : drop SingularPersistentAttribute from extends list
 * 		only 2 out of the 3 identifier forms is a SingularPersistentAttribute, the 3rd is the
 * 		"non-aggregated cid" form.  Granted we could model the 3rd as a
 * 		{@link VirtualPersistentAttribute} (or a {@link VirtualNavigable}) but the underlying issue
 * 		is the abstract base class ({@link AbstractSingularPersistentAttribute}) and what we can pass
 * 		to its ctor - ideally we'd pass the boot-model form
 * 		({@link org.hibernate.boot.model.domain.PersistentAttributeMapping}) which does not exist
 * 		for this 3rd ide form.
 *
 * @author Steve Ebersole
 */
public interface EntityIdentifier<O,J> extends Navigable<J>, AllowableParameterType<J> {
	String NAVIGABLE_ID = "{id}";
	String LEGACY_NAVIGABLE_ID = "id";

	/**
	 * Is this identifier defined by a single attribute on the entity?
	 * <p/>
	 * The only time this is false is in the case of a non-aggregated composite identifier.
	 *
	 * @return {@code false} indicates we have a non-aggregated composite identifier.
	 */
	default boolean hasSingleIdAttribute() {
		return this instanceof SingularPersistentAttribute;
	}

	/**
	 * Get a SingularPersistentAttribute representation of the identifier.
	 * <p/>
	 * Note that for the case of a non-aggregated composite identifier this returns a
	 * "virtual" attribute mapping ({@link VirtualPersistentAttribute})
	 *
	 * @todo (6.0) : decide on what we want to do here.
	 * 		ultimately this is used to implement {@link javax.persistence.metamodel.IdentifiableType#getId}
	 * 		and {@link javax.persistence.metamodel.IdentifiableType#getDeclaredId}.  However, the
	 * 		"tricky" case here (non-aggregate cid) is one that JPA says should throw an
	 * 		exception
	 */
	<T> SingularPersistentAttribute<O,T> asAttribute(Class<T> javaType);

	IdentifierGenerator getIdentifierValueGenerator();

	/**
	 * Retrieve the columns making up the identifier
	 */
	List<Column> getColumns();

	@Override
	default int getNumberOfJdbcParametersToBind() {
		return getColumns().size();
	}

	@Override
	default List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier,
			SqlSelectionGroupResolutionContext resolutionContext) {
		final ArrayList<ColumnReference> columnRefs = new ArrayList<>();
		for ( Column column : getColumns() ) {
			columnRefs.add(
					(ColumnReference) resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
							qualifier,
							column
					)
			);
		}
		return columnRefs;
	}

	boolean matchesNavigableName(String navigableName);
}
