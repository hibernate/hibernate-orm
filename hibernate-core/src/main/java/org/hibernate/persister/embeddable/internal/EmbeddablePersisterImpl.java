/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.embeddable.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.AbstractManagedType;
import org.hibernate.persister.common.spi.Attribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.embeddable.spi.EmbeddableContainer;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.type.descriptor.java.internal.EmbeddableJavaDescriptorImpl;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;
import org.hibernate.type.internal.EmbeddedTypeImpl;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class EmbeddablePersisterImpl<T> extends AbstractManagedType<T> implements EmbeddablePersister<T> {
	private final EmbeddableContainer source;
	private final String localName;

	private final String roleName;
	private final EmbeddedType ormType;

	public EmbeddablePersisterImpl(
			Component componentBinding,
			EmbeddableContainer source,
			String localName,
			PersisterCreationContext creationContext) {
		super( resolveJtd( creationContext, componentBinding ) );
		this.source = source;
		this.localName = localName;

		this.roleName = source.getRolePrefix() + '.' + localName;
		this.ormType = new EmbeddedTypeImpl( null, roleName, getJavaTypeDescriptor() );

		setTypeConfiguration( creationContext.getTypeConfiguration() );
	}

	@SuppressWarnings("unchecked")
	private static <T> EmbeddableJavaDescriptor<T> resolveJtd(PersisterCreationContext creationContext, Component embeddedMapping) {
		JavaTypeDescriptorRegistry jtdr = creationContext.getTypeConfiguration().getJavaTypeDescriptorRegistry();
		EmbeddableJavaDescriptor jtd = (EmbeddableJavaDescriptor) jtdr.getDescriptor( embeddedMapping.getType().getName() );
		if ( jtd == null ) {
			jtd = new EmbeddableJavaDescriptorImpl(
					embeddedMapping.getType().getName(),
					embeddedMapping.getType().getReturnedClass(),
					null
			);
			jtdr.addDescriptor( jtd );
		}
		return jtd;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EmbeddableJavaDescriptor<T> getJavaTypeDescriptor() {
		return (EmbeddableJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public void afterInitialization(
			Component embeddableBinding,
			PersisterCreationContext creationContext) {
		bindAttributes( embeddableBinding, creationContext );
	}

	@Override
	public String getNavigableName() {
		return localName;
	}

	@Override
	public String getRoleName() {
		return roleName;
	}

	@Override
	public String getRolePrefix() {
		return roleName;
	}

	@Override
	public EmbeddablePersister getEmbeddablePersister() {
		return this;
	}

	@Override
	public List<Column> collectColumns() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EmbeddableContainer<?> getSource() {
		return source;
	}

	@Override
	public EmbeddedType getOrmType() {
		return null;
	}

	private void bindAttributes(Component embeddableBinding, PersisterCreationContext creationContext) {
		for ( Property property : embeddableBinding.getDeclaredProperties() ) {

			// todo : Columns
			final List<Column> columns = Collections.emptyList();

			final Attribute attribute = PersisterHelper.INSTANCE.buildAttribute(
					creationContext,
					this,
					embeddableBinding,
					mappingProperty.getName(),
					OrmTypeHelper.convert(
							creationContext,
							this,
							locaName,
							mappingProperty.getValue(),
							creationContext.getTypeConfiguration()
					),
					columns
			);
			addAttribute( attribute );
		}
	}
}
