/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.JoinedIterator;

/**
 * @author Gavin King
 */
public class SingleTableSubclass extends Subclass {

	public SingleTableSubclass(
			EntityMapping superclass,
			MetadataBuildingContext metadataBuildingContext) {
		super( superclass, metadataBuildingContext );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Iterator getNonDuplicatedPropertyIterator() {
		return new JoinedIterator(
				getSuperclass().getUnjoinedPropertyIterator(),
				getUnjoinedPropertyIterator()
		);
	}

	@Override
	protected List<MappedColumn> getDiscriminatorColumns() {
		if ( isDiscriminatorInsertable() && !getDiscriminator().hasFormula() ) {
			return getDiscriminator().getMappedColumns();
		}
		else {
			return super.getDiscriminatorColumns();
		}
	}

	@Override
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept( this );
	}

	@Override
	public void validate() throws MappingException {
		if ( getDiscriminator() == null ) {
			throw new MappingException(
					"No discriminator found for " + getEntityName()
							+ ". Discriminator is needed when 'single-table-per-hierarchy' "
							+ "is used and a class has subclasses"
			);
		}
		super.validate();
	}
}
