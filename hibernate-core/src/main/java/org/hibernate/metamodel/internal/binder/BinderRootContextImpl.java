/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.binder;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.spi.BaseDelegatingBindingContext;
import org.hibernate.metamodel.spi.BindingContext;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HierarchyDetails;

/**
 * @author Steve Ebersole
 */
public class BinderRootContextImpl extends BaseDelegatingBindingContext implements BinderRootContext {
	private final SourceIndex sourceIndex;
	private final BinderEventBus eventBus;
	private Map<EntityHierarchySource,HierarchyDetails> hierarchySourceToBindingMap
			= new HashMap<EntityHierarchySource, HierarchyDetails>();
	private Map<EntitySource,EntityBinding> entitySourceToBindingMap
			= new HashMap<EntitySource, EntityBinding>();

	private final HibernateTypeHelper typeHelper; // todo: refactor helper and remove redundant methods in this class
	private final RelationalIdentifierHelper relationalIdentifierHelper;
	private final TableHelper tableHelper;
	private final ForeignKeyHelper foreignKeyHelper;
	private final RelationalValueBindingHelper relationalValueBindingHelper;
	private final NaturalIdUniqueKeyHelper naturalIdUniqueKeyHelper;

	private final BinderLocalBindingContextSelector localBindingContextSelector;

	public BinderRootContextImpl(BindingContext parent, SourceIndex sourceIndex, BinderEventBus eventBus) {
		super( parent );
		this.sourceIndex = sourceIndex;
		this.eventBus = eventBus;

		this.typeHelper = new HibernateTypeHelper( this );
		this.relationalIdentifierHelper = new RelationalIdentifierHelper( this );
		this.tableHelper = new TableHelper( this );
		this.foreignKeyHelper = new ForeignKeyHelper( this );
		this.relationalValueBindingHelper = new RelationalValueBindingHelper( this );
		this.naturalIdUniqueKeyHelper = new NaturalIdUniqueKeyHelper( this );
		this.localBindingContextSelector = new BinderLocalBindingContextSelectorImpl( this );
	}

	public BinderEventBus getEventBus() {
		return eventBus;
	}

	@Override
	public SourceIndex getSourceIndex() {
		return sourceIndex;
	}

	public void addMapping(EntityHierarchySource source, HierarchyDetails binding) {
		hierarchySourceToBindingMap.put( source, binding );
	}

	public void addMapping(EntitySource source, EntityBinding binding) {
		entitySourceToBindingMap.put( source, binding );
	}

	@Override
	public HierarchyDetails locateBinding(EntityHierarchySource source) {
		// todo : add missing checks
		return hierarchySourceToBindingMap.get( source );
	}

	@Override
	public EntityBinding locateBinding(EntitySource source) {
		// todo : add missing checks
		return entitySourceToBindingMap.get( source );
	}

	@Override
	public BinderLocalBindingContextSelector getLocalBindingContextSelector() {
		return localBindingContextSelector;
	}

	@Override
	public HibernateTypeHelper typeHelper() {
		return typeHelper;
	}

	@Override
	public RelationalIdentifierHelper relationalIdentifierHelper() {
		return relationalIdentifierHelper;
	}

	@Override
	public TableHelper tableHelper() {
		return tableHelper;
	}

	@Override
	public ForeignKeyHelper foreignKeyHelper() {
		return foreignKeyHelper;
	}

	@Override
	public RelationalValueBindingHelper relationalValueBindingHelper() {
		return relationalValueBindingHelper;
	}

	@Override
	public NaturalIdUniqueKeyHelper naturalIdUniqueKeyHelper() {
		return naturalIdUniqueKeyHelper;
	}

}
