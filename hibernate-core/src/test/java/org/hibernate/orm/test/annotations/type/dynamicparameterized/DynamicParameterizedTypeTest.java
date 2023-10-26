/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.orm.test.annotations.type.dynamicparameterized;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.CustomType;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Daniel Gredler
 * @author Jan-Willem Gmelig Meyling
 * @author Sayra Ranjha
 */
@DomainModel(
		annotatedClasses = { AbstractEntity.class, Entity1.class, Entity2.class }
)
@SessionFactory
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = FunctionContributor.class,
				impl = DynamicParameterizedTypeTest.FunctionContributorImpl.class
		)
)
public class DynamicParameterizedTypeTest {

	@Test
	public void testParameterValues(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Entity1 entity1 = new Entity1();
					entity1.id = new Date( 0 );

					Entity2 entity2 = new Entity2();
					entity2.id = new Date( 0 );

					session.persist( entity1 );
					session.persist( entity2 );
					session.flush();
					session.clear();

					entity1 = session.byId( Entity1.class ).load( entity1.id );
					entity2 = session.byId( Entity2.class ).load( entity2.id );

					assertEquals( "ENTITY1.PROP1", entity1.entity1_Prop1 );
					assertEquals( "ENTITY1.PROP2", entity1.entity1_Prop2 );
					assertEquals( "ENTITY1.PROP3.FOO", entity1.entity1_Prop3 );
					assertEquals( "ENTITY1.PROP4.BAR", entity1.entity1_Prop4 );
					assertEquals( "ENTITY1.PROP5", entity1.entity1_Prop5 );
					assertEquals( "ENTITY1.PROP6", entity1.entity1_Prop6 );

					assertEquals( "ENTITY2.PROP1", entity2.entity2_Prop1 );
					assertEquals( "ENTITY2.PROP2", entity2.entity2_Prop2 );
					assertEquals( "ENTITY2.PROP3", entity2.entity2_Prop3 );
					assertEquals( "ENTITY2.PROP4", entity2.entity2_Prop4 );
					assertEquals( "ENTITY2.PROP5.BLAH", entity2.entity2_Prop5 );
					assertEquals( "ENTITY2.PROP6.YEAH", entity2.entity2_Prop6 );
				}
		);
	}

	@Test
	public void testMetamodel(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			List resultList = session.createQuery("select test_func1(a.entity1_Prop3) from Entity1 a").getResultList();
			assertNotNull(resultList);
		});
	}


	public static class FunctionContributorImpl implements FunctionContributor {
		@Override
		public void contributeFunctions(FunctionContributions functionContributions) {
			functionContributions.getFunctionRegistry()
					.patternDescriptorBuilder( "test_func1", "?1" )
					.setReturnTypeResolver(new FunctionReturnTypeResolver() {
						@Override
						public ReturnableType<?> resolveFunctionReturnType(
								ReturnableType<?> impliedType,
								List<? extends SqmTypedNode<?>> arguments,
								TypeConfiguration typeConfiguration) {
							return resolveFunctionReturnType( impliedType, null, arguments, typeConfiguration );
						}

						@Override
						public ReturnableType<?> resolveFunctionReturnType(
								ReturnableType<?> impliedType,
								Supplier<MappingModelExpressible<?>> inferredTypeSupplier,
								List<? extends SqmTypedNode<?>> arguments,
								TypeConfiguration typeConfiguration) {
							SqmTypedNode<?> sqmTypedNode = arguments.get(0);
							BasicDomainType sqmPathType = ((BasicSqmPathSource) sqmTypedNode.getNodeType()).getSqmPathType();
							assertInstanceOf(CustomType.class, sqmPathType);
							return null;
						}

						@Override
						public BasicValuedMapping resolveFunctionReturnType(
								Supplier<BasicValuedMapping> impliedTypeAccess,
								List<? extends SqlAstNode> arguments) {
							return null;
						}
					})
					.register();
		}
	}
}
