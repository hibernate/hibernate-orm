/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;

/**
 * @author Steve Ebersole
 */
public class RuntimeMetamodelsImpl implements RuntimeMetamodelsImplementor {
	private JpaMetamodelImplementor jpaMetamodel;
	private MappingMetamodelImplementor mappingMetamodel;

	public RuntimeMetamodelsImpl() {
	}

	@Override
	public JpaMetamodelImplementor getJpaMetamodel() {
		return jpaMetamodel;
	}

	@Override
	public MappingMetamodelImplementor getMappingMetamodel() {
		return mappingMetamodel;
	}

	@Override
	public EmbeddableValuedModelPart getEmbedded(String role) {
		throw new UnsupportedOperationException( "Locating EmbeddableValuedModelPart by (String) role is not supported" );
	}

	@Override
	public EmbeddableValuedModelPart getEmbedded(NavigableRole role) {
		return mappingMetamodel.getEmbeddableValuedModelPart( role );
	}

	public void setMappingMetamodel(MappingMetamodelImplementor mappingMetamodel) {
		this.mappingMetamodel = mappingMetamodel;
	}

	public void setJpaMetamodel(JpaMetamodelImplementor jpaMetamodel) {
		this.jpaMetamodel = jpaMetamodel;
	}
}
