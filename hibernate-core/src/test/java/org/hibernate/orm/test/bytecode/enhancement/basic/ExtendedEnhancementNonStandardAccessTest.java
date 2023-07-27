package org.hibernate.orm.test.bytecode.enhancement.basic;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

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
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true, extendedEnhancement = true)
public class ExtendedEnhancementNonStandardAccessTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				MyAbstractEntity.class, MyAbstractConfusingEntity.class, MyConcreteEntity.class
		};
	}

	@Test
	public void nonStandardInstanceGetterSetterPublicField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForPublicField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForPublicField();
			}
		} );
	}

	@Test
	public void nonStandardInstanceGetterSetterProtectedField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForProtectedField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForProtectedField();
			}
		} );
	}

	@Test
	public void nonStandardInstanceGetterSetterPackagePrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForPackagePrivateField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForPackagePrivateField();
			}
		} );
	}

	@Test
	public void nonStandardInstanceGetterSetterPrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForPrivateField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForPrivateField();
			}
		} );
	}

	@Test
	public void staticGetterSetterPublicField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.staticSetPublicField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.staticGetPublicField( entity );
			}
		} );
	}

	@Test
	public void staticGetterSetterProtectedField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.staticSetProtectedField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.staticGetProtectedField( entity );
			}
		} );
	}

	@Test
	public void staticGetterSetterPackagePrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.staticSetPackagePrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.staticGetPackagePrivateField( entity );
			}
		} );
	}

	@Test
	public void staticGetterSetterPrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.staticSetPrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.staticGetPrivateField( entity );
			}
		} );
	}

	@Test
	public void innerClassStaticGetterSetterPublicField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.InnerClassAccessors.staticSetPublicField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.InnerClassAccessors.staticGetPublicField( entity );
			}
		} );
	}

	@Test
	public void innerClassStaticGetterSetterProtectedField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.InnerClassAccessors.staticSetProtectedField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.InnerClassAccessors.staticGetProtectedField( entity );
			}
		} );
	}

	@Test
	public void innerClassStaticGetterSetterPackagePrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.InnerClassAccessors.staticSetPackagePrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.InnerClassAccessors.staticGetPackagePrivateField( entity );
			}
		} );
	}

	@Test
	public void innerClassStaticGetterSetterPrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				MyConcreteEntity.InnerClassAccessors.staticSetPrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return MyConcreteEntity.InnerClassAccessors.staticGetPrivateField( entity );
			}
		} );
	}

	@Test
	public void innerClassInstanceGetterSetterPublicField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new MyConcreteEntity.InnerClassAccessors().instanceSetPublicField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new MyConcreteEntity.InnerClassAccessors().instanceGetPublicField( entity );
			}
		} );
	}

	@Test
	public void innerClassInstanceGetterSetterProtectedField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new MyConcreteEntity.InnerClassAccessors().instanceSetProtectedField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new MyConcreteEntity.InnerClassAccessors().instanceGetProtectedField( entity );
			}
		} );
	}

	@Test
	public void innerClassInstanceGetterSetterPackagePrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new MyConcreteEntity.InnerClassAccessors().instanceSetPackagePrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new MyConcreteEntity.InnerClassAccessors().instanceGetPackagePrivateField( entity );
			}
		} );
	}

	@Test
	public void innerClassInstanceGetterSetterPrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new MyConcreteEntity.InnerClassAccessors().instanceSetPrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new MyConcreteEntity.InnerClassAccessors().instanceGetPrivateField( entity );
			}
		} );
	}

	@Test
	public void externalClassStaticGetterSetterPublicField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				ExternalClassAccessors.staticSetPublicField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return ExternalClassAccessors.staticGetPublicField( entity );
			}
		} );
	}

	@Test
	public void externalClassStaticGetterSetterProtectedField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				ExternalClassAccessors.staticSetProtectedField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return ExternalClassAccessors.staticGetProtectedField( entity );
			}
		} );
	}

	@Test
	public void externalClassStaticGetterSetterPackagePrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				ExternalClassAccessors.staticSetPackagePrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return ExternalClassAccessors.staticGetPackagePrivateField( entity );
			}
		} );
	}

	@Test
	public void externalClassInstanceGetterSetterPublicField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new ExternalClassAccessors().instanceSetPublicField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new ExternalClassAccessors().instanceGetPublicField( entity );
			}
		} );
	}

	@Test
	public void externalClassInstanceGetterSetterProtectedField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new ExternalClassAccessors().instanceSetProtectedField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new ExternalClassAccessors().instanceGetProtectedField( entity );
			}
		} );
	}

	@Test
	public void externalClassInstanceGetterSetterPackagePrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				new ExternalClassAccessors().instanceSetPackagePrivateField( entity, value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return new ExternalClassAccessors().instanceGetPackagePrivateField( entity );
			}
		} );
	}

	@Test
	public void subClassInstanceGetterSetterPublicField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.setAbstractEntityPublicField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.getAbstractEntityPublicField();
			}
		} );
	}

	@Test
	public void subClassInstanceGetterSetterProtectedField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.setAbstractEntityProtectedField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.getAbstractEntityProtectedField();
			}
		} );
	}

	@Test
	public void subClassInstanceGetterSetterPackagePrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.setAbstractEntityPackagePrivateField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.getAbstractEntityPackagePrivateField();
			}
		} );
	}

	@Test
	public void subClassNonStandardInstanceGetterSetterPublicField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForAbstractEntityPublicField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForAbstractEntityPublicField();
			}
		} );
	}

	@Test
	public void subClassNonStandardInstanceGetterSetterProtectedField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForAbstractEntityProtectedField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForAbstractEntityProtectedField();
			}
		} );
	}

	@Test
	public void subClassNonStandardInstanceGetterSetterPackagePrivateField() {
		doTestFieldAccess( new AccessDelegate() {
			@Override
			public void setValue(MyConcreteEntity entity, Long value) {
				entity.nonStandardSetterForAbstractEntityPackagePrivateField( value );
			}

			@Override
			public Long getValue(MyConcreteEntity entity) {
				return entity.nonStandardGetterForAbstractEntityPackagePrivateField();
			}
		} );
	}

	// Ideally we'd make this a @ParameterizedTest and pass the access delegate as parameter,
	// but we cannot do that due to JUnit using a different classloader than the test.
	private void doTestFieldAccess(AccessDelegate delegate) {
		Long id = fromTransaction( em -> {
			var entity = new MyConcreteEntity();
			em.persist( entity );
			return entity.id;
		} );

		inTransaction( em -> {
			var entity = em.find( MyConcreteEntity.class, id );
			assertThat( delegate.getValue( entity ) )
					.as( "Loaded value before update" )
					.isNull();
		} );

		inTransaction( em -> {
			var entity = em.getReference( MyConcreteEntity.class, id );
			// Since field access is replaced with accessor calls,
			// we expect this change to be detected by dirty tracking and persisted.
			delegate.setValue( entity, 42L );
		} );

		inTransaction( em -> {
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

		inTransaction( em -> {
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
