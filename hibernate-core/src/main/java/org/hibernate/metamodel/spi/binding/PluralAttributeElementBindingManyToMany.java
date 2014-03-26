/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.binding;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.relational.ForeignKey;

/**
 * Describes plural attributes of {@link org.hibernate.metamodel.spi.PluralAttributeElementNature#MANY_TO_MANY} elements
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class PluralAttributeElementBindingManyToMany extends AbstractPluralAttributeAssociationElementBinding implements Filterable{
	private List<FilterConfiguration> filterConfigurations = new ArrayList<FilterConfiguration>();
	private String manyToManyWhere;
	private String manyToManyOrderBy;
	private FetchMode fetchMode;
	private JoinRelationalValueBindingContainer relationalValueBindingContainer;

	PluralAttributeElementBindingManyToMany(AbstractPluralAttributeBinding binding) {
		super( binding );
	}

	@Override
	public RelationalValueBindingContainer getRelationalValueContainer() {
		return relationalValueBindingContainer;
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.MANY_TO_MANY;
	}

	public void setJoinRelationalValueBindings(
			List<RelationalValueBinding> relationalValueBindings,
			ForeignKey foreignKey) {
		this.relationalValueBindingContainer = new JoinRelationalValueBindingContainer(
				relationalValueBindings,
				foreignKey
		);
	}

	public ForeignKey getForeignKey() {
		return relationalValueBindingContainer.getForeignKey();
	}

	public String getManyToManyWhere() {
		return manyToManyWhere;
	}

	public void setManyToManyWhere(String manyToManyWhere) {
		this.manyToManyWhere = manyToManyWhere;
	}

	public String getManyToManyOrderBy() {
		return manyToManyOrderBy;
	}

	public void setManyToManyOrderBy(String manyToManyOrderBy) {
		this.manyToManyOrderBy = manyToManyOrderBy;
	}

	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode = fetchMode;
	}

	@Override
	public void addFilterConfiguration(FilterConfiguration filterConfiguration) {
		filterConfigurations.add( filterConfiguration );
	}

	@Override
	public List<FilterConfiguration> getFilterConfigurations() {
		return filterConfigurations;
	}
}
