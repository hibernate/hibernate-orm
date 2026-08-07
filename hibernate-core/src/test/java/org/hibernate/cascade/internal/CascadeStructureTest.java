/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the consolidated single-walker cascade structure.
///
/// @author Steve Ebersole
class CascadeStructureTest {
	private static final String[] REMOVED_TYPES = {
			"org.hibernate.cascade.internal.LegacyCascadeWalker",
			"org.hibernate.cascade.internal.CascadePlanWalker",
			"org.hibernate.cascade.internal.CascadeRouteProfile",
			"org.hibernate.cascade.internal.plan.CascadeCollectionElementPlan",
			"org.hibernate.cascade.internal.plan.CascadePlan",
			"org.hibernate.cascade.internal.plan.CascadePlanAccess",
			"org.hibernate.cascade.internal.plan.CascadePlanCompiler",
			"org.hibernate.cascade.internal.plan.CascadePlanNode",
			"org.hibernate.cascade.internal.plan.CascadePlanRegistry"
	};

	@Test
	void oneConcreteWalkerAndOnePropertyProcessorRemain() {
		assertThat( CascadeWalker.class.isInterface() ).isFalse();
		assertThat( Modifier.isFinal( CascadeWalker.class.getModifiers() ) ).isTrue();

		assertThat( Arrays.stream( Cascade.class.getDeclaredMethods() )
				.filter( method -> method.getName().equals( "cascadeProperty" ) ) )
				.singleElement();
		assertThat( Arrays.stream( Cascade.class.getDeclaredMethods() )
				.flatMap( method -> Arrays.stream( method.getParameterTypes() ) ) )
				.noneMatch( parameterType -> parameterType == CascadeWalker.class );
	}

	@Test
	void planAndWalkerSelectionInfrastructureIsAbsent() {
		assertThat( Arrays.stream( Cascade.class.getDeclaredFields() )
				.map( field -> field.getType() ) )
				.noneMatch( fieldType -> fieldType == ThreadLocal.class );
		assertThat( Arrays.stream( MappingMetamodelImpl.class.getDeclaredFields() )
				.map( field -> field.getName().toLowerCase() ) )
				.noneMatch( name -> name.contains( "cascadeplan" ) );

		for ( String removedType : REMOVED_TYPES ) {
			assertThat( isPresent( removedType ) )
					.as( removedType )
					.isFalse();
		}
	}

	private static boolean isPresent(String className) {
		try {
			Class.forName( className, false, CascadeStructureTest.class.getClassLoader() );
			return true;
		}
		catch (ClassNotFoundException expected) {
			return false;
		}
	}
}
