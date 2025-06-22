/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Checks that non-standard access to fields by the application works correctly,
 * i.e. when exposing fields through accessors with non-standard names,
 * static accessors, accessors defined in a subclass,
 * or accessors defined in an inner class.
 */
@DomainModel(
		annotatedClasses = {
			ExtendedEnhancementNonStandardAccessTest.MyAbstractEntity.class, ExtendedEnhancementNonStandardAccessTest.MyAbstractConfusingEntity.class, ExtendedEnhancementNonStandardAccessTest.MyConcreteEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, extendedEnhancement = true)
public class ExtendedEnhancementNonStandardAccessTest {

	@Test
	public void nonStandardInstanceGetterSetterPublicField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForPublicField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForPublicField();
			}
		}, scope );
	}

	@Test
	public void nonStandardInstanceGetterSetterProtectedField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForProtectedField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForProtectedField();
			}
		}, scope );
	}

	@Test
	public void nonStandardInstanceGetterSetterPackagePrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForPackagePrivateField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForPackagePrivateField();
			}
		}, scope );
	}

	@Test
	public void nonStandardInstanceGetterSetterPrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForPrivateField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForPrivateField();
			}
		}, scope );
	}

	@Test
	public void staticGetterSetterPublicField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.staticSetPublicField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.staticGetPublicField( entity );
			}
		}, scope );
	}

	@Test
	public void staticGetterSetterProtectedField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.staticSetProtectedField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.staticGetProtectedField( entity );
			}
		}, scope );
	}

	@Test
	public void staticGetterSetterPackagePrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.staticSetPackagePrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.staticGetPackagePrivateField( entity );
			}
		}, scope );
	}

	@Test
	public void staticGetterSetterPrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.staticSetPrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.staticGetPrivateField( entity );
			}
		}, scope );
	}

	@Test
	public void innerClassStaticGetterSetterPublicField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.InnerClassAccessors.staticSetPublicField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.InnerClassAccessors.staticGetPublicField( entity );
			}
		}, scope );
	}

	@Test
	public void innerClassStaticGetterSetterProtectedField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.InnerClassAccessors.staticSetProtectedField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.InnerClassAccessors.staticGetProtectedField( entity );
			}
		}, scope );
	}

	@Test
	public void innerClassStaticGetterSetterPackagePrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.InnerClassAccessors.staticSetPackagePrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.InnerClassAccessors.staticGetPackagePrivateField( entity );
			}
		}, scope );
	}

	@Test
	public void innerClassStaticGetterSetterPrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.InnerClassAccessors.staticSetPrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.InnerClassAccessors.staticGetPrivateField( entity );
			}
		}, scope );
	}

	@Test
	public void innerClassInstanceGetterSetterPublicField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new MyConcreteEntity.InnerClassAccessors().instanceSetPublicField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new MyConcreteEntity.InnerClassAccessors().instanceGetPublicField( entity );
			}
		}, scope );
	}

	@Test
	public void innerClassInstanceGetterSetterProtectedField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new MyConcreteEntity.InnerClassAccessors().instanceSetProtectedField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new MyConcreteEntity.InnerClassAccessors().instanceGetProtectedField( entity );
			}
		}, scope );
	}

	@Test
	public void innerClassInstanceGetterSetterPackagePrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new MyConcreteEntity.InnerClassAccessors().instanceSetPackagePrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new MyConcreteEntity.InnerClassAccessors().instanceGetPackagePrivateField( entity );
			}
		}, scope );
	}

	@Test
	public void innerClassInstanceGetterSetterPrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new MyConcreteEntity.InnerClassAccessors().instanceSetPrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new MyConcreteEntity.InnerClassAccessors().instanceGetPrivateField( entity );
			}
		}, scope );
	}

	@Test
	public void externalClassStaticGetterSetterPublicField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				ExternalClassAccessors.staticSetPublicField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return ExternalClassAccessors.staticGetPublicField( entity );
			}
		}, scope );
	}

	@Test
	public void externalClassStaticGetterSetterProtectedField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				ExternalClassAccessors.staticSetProtectedField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return ExternalClassAccessors.staticGetProtectedField( entity );
			}
		}, scope );
	}

	@Test
	public void externalClassStaticGetterSetterPackagePrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				ExternalClassAccessors.staticSetPackagePrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return ExternalClassAccessors.staticGetPackagePrivateField( entity );
			}
		}, scope );
	}

	@Test
	public void externalClassInstanceGetterSetterPublicField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new ExternalClassAccessors().instanceSetPublicField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new ExternalClassAccessors().instanceGetPublicField( entity );
			}
		}, scope );
	}

	@Test
	public void externalClassInstanceGetterSetterProtectedField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new ExternalClassAccessors().instanceSetProtectedField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new ExternalClassAccessors().instanceGetProtectedField( entity );
			}
		}, scope );
	}

	@Test
	public void externalClassInstanceGetterSetterPackagePrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new ExternalClassAccessors().instanceSetPackagePrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new ExternalClassAccessors().instanceGetPackagePrivateField( entity );
			}
		}, scope );
	}

	@Test
	public void subClassInstanceGetterSetterPublicField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.setAbstractEntityPublicField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.getAbstractEntityPublicField();
			}
		}, scope );
	}

	@Test
	public void subClassInstanceGetterSetterProtectedField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.setAbstractEntityProtectedField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.getAbstractEntityProtectedField();
			}
		}, scope );
	}

	@Test
	public void subClassInstanceGetterSetterPackagePrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.setAbstractEntityPackagePrivateField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.getAbstractEntityPackagePrivateField();
			}
		}, scope );
	}

	@Test
	public void subClassNonStandardInstanceGetterSetterPublicField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForAbstractEntityPublicField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForAbstractEntityPublicField();
			}
		}, scope );
	}

	@Test
	public void subClassNonStandardInstanceGetterSetterProtectedField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForAbstractEntityProtectedField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForAbstractEntityProtectedField();
			}
		}, scope );
	}

	@Test
	public void subClassNonStandardInstanceGetterSetterPackagePrivateField(SessionFactoryScope scope) {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForAbstractEntityPackagePrivateField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForAbstractEntityPackagePrivateField();
			}
		}, scope );
	}

	// Ideally we'd make this a @ParameterizedTest and pass the access delegate as parameter,
	// but we cannot do that due to JUnit using a different classloader than the test.
	private void doTestFieldAccess(AccessDelegate delegate, SessionFactoryScope scope) {
		Long id = scope.fromTransaction( em -> {
			var entity = new MyConcreteEntity();
			em.persist( entity );
			return entity.id;
		} );

		scope.inTransaction( em -> {
			var entity = em.find( MyConcreteEntity.class, id );
			assertThat( delegate.getValue( entity ) )
					.as( "Loaded value before update" )
					.isNull();
		} );

		scope.inTransaction( em -> {
			var entity = em.getReference( MyConcreteEntity.class, id );
			// Since field access is replaced with accessor calls,
			// we expect this change to be detected by dirty tracking and persisted.
			delegate.setValue( entity, 42L );
		} );

		scope.inTransaction( em -> {
			var entity = em.find( MyConcreteEntity.class, id );
			// We're working on an initialized entity.
			assertThat( entity )
					.as( "find() should return uninitialized entity" )
					.returns( true, Hibernate::isInitialized );
			// The above should have persisted a value that passes the assertion.
			assertThat( delegate.getValue( entity ) )
					.as( "Loaded value after update" )
					.isEqualTo( 42L );
		} );

		scope.inTransaction( em -> {
			var entity = em.getReference( MyConcreteEntity.class, id );
			// We're working on an uninitialized entity.
			assertThat( entity )
					.as( "getReference() should return uninitialized entity" )
					.returns( false, Hibernate::isInitialized );
			// The above should have persisted a value that passes the assertion.
			assertThat( delegate.getValue( entity ) )
					.as( "Lazily loaded value after update" )
					.isEqualTo( 42L );
			// Accessing the value should trigger initialization of the entity.
			assertThat( entity )
					.as( "Getting the value should initialize the entity" )
					.returns( true, Hibernate::isInitialized );
		} );
	}

	@Entity(name = "abstract")
	public static abstract class MyAbstractEntity {
		@Id
		@GeneratedValue
		public long id;
		@Column(name = "abstract_public_field")
		public Long abstractEntityPublicField;
		@Column(name = "abstract_protected_field")
		protected Long abstractEntityProtectedField;
		@Column(name = "abstract_default_field")
		Long abstractEntityPackagePrivateField;
	}

	// Just to confuse panache: the fields are not declared in the *direct* superclass.
	@Entity(name = "abstract2")
	public static abstract class MyAbstractConfusingEntity extends MyAbstractEntity {
	}

	@Entity(name = "concrete")
	public static class MyConcreteEntity extends MyAbstractConfusingEntity {
		@Column(name = "concrete_public_field")
		public Long publicField;
		@Column(name = "concrete_protected_field")
		protected Long protectedField;
		@Column(name = "concrete_default_field")
		Long packagePrivateField;
		@Column(name = "concrete_private_field")
		private Long privateField;

		public Long getAbstractEntityPublicField() {
			return abstractEntityPublicField;
		}

		public void setAbstractEntityPublicField(Long abstractEntityPublicField) {
			this.abstractEntityPublicField = abstractEntityPublicField;
		}

		public Long nonStandardGetterForAbstractEntityPublicField() {
			return abstractEntityPublicField;
		}

		public void nonStandardSetterForAbstractEntityPublicField(Long value) {
			abstractEntityPublicField = value;
		}

		public Long getAbstractEntityProtectedField() {
			return abstractEntityProtectedField;
		}

		public void setAbstractEntityProtectedField(Long abstractEntityProtectedField) {
			this.abstractEntityProtectedField = abstractEntityProtectedField;
		}

		public Long nonStandardGetterForAbstractEntityProtectedField() {
			return abstractEntityProtectedField;
		}

		public void nonStandardSetterForAbstractEntityProtectedField(Long value) {
			abstractEntityProtectedField = value;
		}

		public Long getAbstractEntityPackagePrivateField() {
			return abstractEntityPackagePrivateField;
		}

		public void setAbstractEntityPackagePrivateField(Long abstractEntityPackagePrivateField) {
			this.abstractEntityPackagePrivateField = abstractEntityPackagePrivateField;
		}

		public Long nonStandardGetterForAbstractEntityPackagePrivateField() {
			return abstractEntityPackagePrivateField;
		}

		public void nonStandardSetterForAbstractEntityPackagePrivateField(Long value) {
			abstractEntityPackagePrivateField = value;
		}

		public Long nonStandardGetterForPublicField() {
			return publicField;
		}

		public void nonStandardSetterForPublicField(Long value) {
			publicField = value;
		}

		public Long nonStandardGetterForPackagePrivateField() {
			return packagePrivateField;
		}

		public void nonStandardSetterForPackagePrivateField(Long value) {
			packagePrivateField = value;
		}

		public Long nonStandardGetterForProtectedField() {
			return protectedField;
		}

		public void nonStandardSetterForProtectedField(Long value) {
			protectedField = value;
		}

		public Long nonStandardGetterForPrivateField() {
			return privateField;
		}

		public void nonStandardSetterForPrivateField(Long value) {
			privateField = value;
		}

		public static Long staticGetPublicField(MyConcreteEntity entity) {
			return entity.publicField;
		}

		public static void staticSetPublicField(MyConcreteEntity entity, Long value) {
			entity.publicField = value;
		}

		public static Long staticGetProtectedField(MyConcreteEntity entity) {
			return entity.protectedField;
		}

		public static void staticSetProtectedField(MyConcreteEntity entity, Long value) {
			entity.protectedField = value;
		}

		public static Long staticGetPackagePrivateField(MyConcreteEntity entity) {
			return entity.packagePrivateField;
		}

		public static void staticSetPackagePrivateField(MyConcreteEntity entity, Long value) {
			entity.packagePrivateField = value;
		}

		public static Long staticGetPrivateField(MyConcreteEntity entity) {
			return entity.privateField;
		}

		public static void staticSetPrivateField(MyConcreteEntity entity, Long value) {
			entity.privateField = value;
		}

		public static final class InnerClassAccessors {
			public static Long staticGetPublicField(MyConcreteEntity entity) {
				return entity.publicField;
			}

			public static void staticSetPublicField(MyConcreteEntity entity, Long value) {
				entity.publicField = value;
			}

			public static Long staticGetProtectedField(MyConcreteEntity entity) {
				return entity.protectedField;
			}

			public static void staticSetProtectedField(MyConcreteEntity entity, Long value) {
				entity.protectedField = value;
			}

			public static Long staticGetPackagePrivateField(MyConcreteEntity entity) {
				return entity.packagePrivateField;
			}

			public static void staticSetPackagePrivateField(MyConcreteEntity entity, Long value) {
				entity.packagePrivateField = value;
			}

			public static Long staticGetPrivateField(MyConcreteEntity entity) {
				return entity.privateField;
			}

			public static void staticSetPrivateField(MyConcreteEntity entity, Long value) {
				entity.privateField = value;
			}

			public Long instanceGetPublicField(MyConcreteEntity entity) {
				return entity.publicField;
			}

			public void instanceSetPublicField(MyConcreteEntity entity, Long value) {
				entity.publicField = value;
			}

			public Long instanceGetProtectedField(MyConcreteEntity entity) {
				return entity.protectedField;
			}

			public void instanceSetProtectedField(MyConcreteEntity entity, Long value) {
				entity.protectedField = value;
			}

			public Long instanceGetPackagePrivateField(MyConcreteEntity entity) {
				return entity.packagePrivateField;
			}

			public void instanceSetPackagePrivateField(MyConcreteEntity entity, Long value) {
				entity.packagePrivateField = value;
			}

			public Long instanceGetPrivateField(MyConcreteEntity entity) {
				return entity.privateField;
			}

			public void instanceSetPrivateField(MyConcreteEntity entity, Long value) {
				entity.privateField = value;
			}
		}
	}

	public static final class ExternalClassAccessors {
		public static Long staticGetPublicField(MyConcreteEntity entity) {
			return entity.publicField;
		}

		public static void staticSetPublicField(MyConcreteEntity entity, Long value) {
			entity.publicField = value;
		}

		public static Long staticGetProtectedField(MyConcreteEntity entity) {
			return entity.protectedField;
		}

		public static void staticSetProtectedField(MyConcreteEntity entity, Long value) {
			entity.protectedField = value;
		}

		public static Long staticGetPackagePrivateField(MyConcreteEntity entity) {
			return entity.packagePrivateField;
		}

		public static void staticSetPackagePrivateField(MyConcreteEntity entity, Long value) {
			entity.packagePrivateField = value;
		}

		public Long instanceGetPublicField(MyConcreteEntity entity) {
			return entity.publicField;
		}

		public void instanceSetPublicField(MyConcreteEntity entity, Long value) {
			entity.publicField = value;
		}

		public Long instanceGetProtectedField(MyConcreteEntity entity) {
			return entity.protectedField;
		}

		public void instanceSetProtectedField(MyConcreteEntity entity, Long value) {
			entity.protectedField = value;
		}

		public Long instanceGetPackagePrivateField(MyConcreteEntity entity) {
			return entity.packagePrivateField;
		}

		public void instanceSetPackagePrivateField(MyConcreteEntity entity, Long value) {
			entity.packagePrivateField = value;
		}
	}

	private interface AccessDelegate {
		void setValue(MyConcreteEntity entity, Long value);

		Long getValue(MyConcreteEntity entity);
	}
}
