/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.MappingDefaults;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.spi.BaseDelegatingBindingContext;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HierarchyDetails;
import org.hibernate.xml.spi.Origin;

/**
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class BinderLocalBindingContextSelectorImpl implements BinderLocalBindingContextSelector {
	private final BinderRootContext rootContext;
	private BinderLocalBindingContextImpl current;

	public BinderLocalBindingContextSelectorImpl(BinderRootContext rootContext) {
		this.rootContext = rootContext;
	}

	@Override
	public BinderLocalBindingContext getCurrentBinderLocalBindingContext() {
		if ( current == null ) {
			throw new AssertionFailure( "No BinderLocalBindingContextImpl currently set" );
		}
		return current;
	}

	public BinderLocalBindingContext setCurrent(EntitySource entitySource) {
		current = new BinderLocalBindingContextImpl( rootContext, entitySource );
		return current;
	}

	public void unsetCurrent() {
		current = null;
	}

	private static class BinderLocalBindingContextImpl
			extends BaseDelegatingBindingContext
			implements BinderLocalBindingContext {

		private final EntitySource source;

		private BinderLocalBindingContextImpl(BinderRootContext parent, EntitySource source) {
			super( parent );
			this.source = source;
		}

		@Override
		protected BinderRootContext parent() {
			return (BinderRootContext) super.parent();
		}

		@Override
		public HierarchyDetails locateBinding(EntityHierarchySource source) {
			return parent().locateBinding( source );
		}

		@Override
		public EntityBinding locateBinding(EntitySource source) {
			return parent().locateBinding( source );
		}

		@Override
		public BinderLocalBindingContextSelector getLocalBindingContextSelector() {
			return parent().getLocalBindingContextSelector();
		}

		@Override
		public BinderEventBus getEventBus() {
			return parent().getEventBus();
		}

		@Override
		public SourceIndex getSourceIndex() {
			return parent().getSourceIndex();
		}

		@Override
		public HibernateTypeHelper typeHelper() {
			return parent().typeHelper();
		}

		@Override
		public RelationalIdentifierHelper relationalIdentifierHelper() {
			return parent().relationalIdentifierHelper();
		}

		@Override
		public TableHelper tableHelper() {
			return parent().tableHelper();
		}

		@Override
		public ForeignKeyHelper foreignKeyHelper() {
			return parent().foreignKeyHelper();
		}

		@Override
		public RelationalValueBindingHelper relationalValueBindingHelper() {
			return parent().relationalValueBindingHelper();
		}

		@Override
		public NaturalIdUniqueKeyHelper naturalIdUniqueKeyHelper() {
			return parent().naturalIdUniqueKeyHelper();
		}

		@Override
		public boolean quoteIdentifiersInContext() {
			// NOTE : for now source.quoteIdentifiersLocalToEntity() only ever returns
			//		TRUE/UNKNOWN.  The logic here accounts for possible future
			//		allowance of selectively disabling global-identifier-quoting.
			switch ( source.quoteIdentifiersLocalToEntity() ) {
				case TRUE: {
					return true;
				}
				case FALSE: {
					return false;
				}
				default: {
					return super.quoteIdentifiersInContext();
				}
			}
		}

		@Override
		public String qualifyClassName(String name) {
			return source.getLocalBindingContext().qualifyClassName( name );
		}

		@Override
		public JavaTypeDescriptor typeDescriptor(String name) {
			return source.getLocalBindingContext().typeDescriptor( name );
		}

		@Override
		public MappingDefaults getMappingDefaults() {
			return source.getLocalBindingContext().getMappingDefaults();
		}

		@Override
		public Origin getOrigin() {
			return source.getLocalBindingContext().getOrigin();
		}

		@Override
		public MappingException makeMappingException(String message) {
			return source.getLocalBindingContext().makeMappingException( message );
		}

		@Override
		public MappingException makeMappingException(String message, Exception cause) {
			return source.getLocalBindingContext().makeMappingException( message, cause );
		}
	}
}