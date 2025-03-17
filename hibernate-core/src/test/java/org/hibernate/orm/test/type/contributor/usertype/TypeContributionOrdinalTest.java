package org.hibernate.orm.test.type.contributor.usertype;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test to assert that service loaded types are processed in ascending ordinal order
 *
 * @author Steven Barendregt
 */
@DomainModel
@SessionFactory
@BootstrapServiceRegistry(
		javaServices = {
				@BootstrapServiceRegistry.JavaService(role = TypeContributor.class, impl = TypeContributionOrdinalTest.HigherOrdinalServiceLoadedVarcharTypeContributor.class),
				@BootstrapServiceRegistry.JavaService(role = TypeContributor.class, impl = TypeContributionOrdinalTest.ServiceLoadedVarcharTypeContributor.class)
		}
)
public class TypeContributionOrdinalTest {

	@Test
	@Jira(value = "https://hibernate.atlassian.net/issues/HHH-19247")
	public void testHigherOrdinalServiceLoadedCustomUserTypeTakesPrecedence(SessionFactoryScope scope) {
		final TypeConfiguration typeConfigurations = scope.getSessionFactory()
				.getMappingMetamodel()
				.getTypeConfiguration();
		Assertions.assertInstanceOf(
				HigherOrdinalServiceLoadedVarcharTypeContributor.HigherOrdinalExtendedVarcharJdbcType.class,
				typeConfigurations.getJdbcTypeRegistry().findDescriptor( 12 )
		);
	}

	public static class ServiceLoadedVarcharTypeContributor implements TypeContributor {

		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			typeContributions.contributeJdbcType( ExtendedVarcharJdbcType.INSTANCE );
		}

		public static class ExtendedVarcharJdbcType extends VarcharJdbcType {

			public static final ExtendedVarcharJdbcType INSTANCE = new ExtendedVarcharJdbcType();
		}

	}

	public static class HigherOrdinalServiceLoadedVarcharTypeContributor implements TypeContributor {

		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			typeContributions.contributeJdbcType( HigherOrdinalExtendedVarcharJdbcType.INSTANCE );
		}

		@Override
		public int ordinal() {
			return 2000;
		}

		public static class HigherOrdinalExtendedVarcharJdbcType extends VarcharJdbcType {

			public static final HigherOrdinalExtendedVarcharJdbcType INSTANCE = new HigherOrdinalExtendedVarcharJdbcType();
		}
	}
}
