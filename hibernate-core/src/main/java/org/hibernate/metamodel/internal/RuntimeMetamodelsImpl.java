/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;

/**
 * @author Steve Ebersole
 */
public class RuntimeMetamodelsImpl implements RuntimeMetamodels {
	private JpaMetamodel jpaMetamodel;
	private MappingMetamodel mappingMetamodel;

	public RuntimeMetamodelsImpl() {
	}

	@Override
	public JpaMetamodel getJpaMetamodel() {
		return jpaMetamodel;
	}

	@Override
	public MappingMetamodel getMappingMetamodel() {
		return mappingMetamodel;
	}

	@Override
	public EmbeddableValuedModelPart getEmbedded(String role) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Chicken-and-egg because things try to use the SessionFactory (specifically the MappingMetamodel)
	 * before it is ready.  So we do this fugly code...
	 */
	public void finishInitialization(
			MetadataImplementor bootMetamodel,
			BootstrapContext bootstrapContext,
			SessionFactoryImpl sessionFactory) {
		final MappingMetamodelImpl mappingMetamodel = bootstrapContext.getTypeConfiguration().scope( sessionFactory );
		this.mappingMetamodel = mappingMetamodel;
		mappingMetamodel.finishInitialization(
				bootMetamodel,
				bootstrapContext,
				sessionFactory
		);

		this.jpaMetamodel = mappingMetamodel.getJpaMetamodel();
	}
}
