/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.criteria.paths;

import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author Andrea Boriero
 */
public class EntityModeMapJoinsTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] {
				getClass().getPackage().getName().replace( '.', '/' ) + "/PolicyAndDistribution.hbm.xml"
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );

		// make sure that dynamic-map mode entity types are returned in the metamodel.
		options.put( AvailableSettings.JPA_METAMODEL_POPULATION, "enabled" );
	}

	/**
	 * When building a join from a non-class based entity (EntityMode.MAP), make sure you get the Bindable from
	 * the SingularAttribute as the join model. If you don't, you'll get the first non-classed based entity
	 * you added to your configuration. Regression for HHH-9142.
	 */
	@Test
	public void testEntityModeMapJoins() throws Exception {
		CriteriaBuilder criteriaBuilder = mock( CriteriaBuilder.class);
		PathSource pathSource = mock( PathSource.class);
		SingularAttribute joinAttribute = mock( SingularAttribute.class);
		when(joinAttribute.getPersistentAttributeType()).thenReturn( Attribute.PersistentAttributeType.MANY_TO_ONE);
		Type joinType = mock( Type.class, withSettings().extraInterfaces( Bindable.class));
		when(joinAttribute.getType()).thenReturn(joinType);
		SingularAttributeJoin join = new SingularAttributeJoin( criteriaBuilder, null, pathSource, joinAttribute, JoinType.LEFT);

		assertEquals( joinType, join.getModel());
	}
}
