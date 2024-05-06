/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.propertyref;

import org.hibernate.annotations.PropertyRef;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ManyToOnePropertyRefTests {
	@Test
	@DomainModel(
			annotatedClasses = {Employee.class, TaxInformation.class},
			xmlMappings = "mappings/models/hbm/propertyref/many-to-one.hbm.xml"
	)
	@SessionFactory
	void testBasicPropertyRefHbm(DomainModelScope modelScope, SessionFactoryScope sfScope) {
		verify( modelScope.getEntityBinding( TaxInformation.class ), sfScope );
	}

	private void verify(PersistentClass entityBinding, SessionFactoryScope sfScope) {
		final Property employeeProp = entityBinding.getProperty( "employee" );
		final ToOne employeePropValue = (ToOne) employeeProp.getValue();
		assertThat( employeePropValue.getReferencedPropertyName() ).isEqualTo( "socialSecurityNumber" );

		sfScope.inTransaction( (session) -> {
			final Employee john = new Employee( 1, "John", "123-45-6789" );
			final TaxInformation taxInformation = new TaxInformation( 1, john, 123.45 );
			session.persist( john );
			session.persist( taxInformation );
		} );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name=MappingSettings.TRANSFORM_HBM_XML, value="true"))
	@DomainModel(
			annotatedClasses = {Employee.class, TaxInformation.class},
			xmlMappings = "mappings/models/hbm/propertyref/many-to-one.hbm.xml"
	)
	@SessionFactory
	void testBasicPropertyRefHbmTransformed(DomainModelScope modelScope, SessionFactoryScope sfScope) {
		verify( modelScope.getEntityBinding( TaxInformation.class ), sfScope );
	}

	@Test
	@DomainModel(annotatedClasses = {Employee.class, TaxInformation.class})
	@SessionFactory
	void testBasicPropertyRef(DomainModelScope modelScope, SessionFactoryScope sfScope) {
		verify( modelScope.getEntityBinding( TaxInformation.class ), sfScope );
	}

	/**
	 * @author Steve Ebersole
	 */
	@Entity(name = "Employee")
	@Table(name = "Employee")
	public static class Employee {
		@Id
		private Integer id;
		private String name;
		private String socialSecurityNumber;

		public Employee() {
		}

		public Employee(Integer id, String name, String socialSecurityNumber) {
			this.id = id;
			this.name = name;
			this.socialSecurityNumber = socialSecurityNumber;
		}
	}

	/**
	 * @author Steve Ebersole
	 */
	@Entity(name = "TaxInformation")
	@Table(name = "TaxInformation")
	public static class TaxInformation {
		@Id
		private Integer id;
		@ManyToOne
		@PropertyRef("socialSecurityNumber")
		private Employee employee;
		private double withholding;

		public TaxInformation() {
		}

		public TaxInformation(Integer id, Employee employee, double withholding) {
			this.id = id;
			this.employee = employee;
			this.withholding = withholding;
		}
	}
}
