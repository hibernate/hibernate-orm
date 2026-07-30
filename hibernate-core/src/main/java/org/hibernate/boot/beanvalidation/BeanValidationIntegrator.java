/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.tool.schema.ValidationConstraintDdlInfluence;

/**
 * Legacy settings holder retained for compatibility.  Bean Validation is now
 * prepared as a {@link BeanValidationPlan} so its relational and runtime phases
 * can run at their proper construction boundaries.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@linkplain BeanValidationPlan} instead.  This class is no longer used internally and
 * will be removed.
 */
@Deprecated(since = "9.0", forRemoval = true)
public class BeanValidationIntegrator implements BeanValidationSettings {

	/**
	 * Used to validate the type of an explicitly passed ValidatorFactory instance.
	 */
	public static void validateFactory(Object object) {
		try {
			final var activatorClass =
					BeanValidationIntegrator.class.getClassLoader().loadClass( ACTIVATOR_CLASS_NAME );
			try {
				activatorClass
						.getMethod( VALIDATE_SUPPLIED_FACTORY_METHOD_NAME, Object.class )
						.invoke( null, object );
			}
			catch (InvocationTargetException e) {
				if ( e.getTargetException() instanceof HibernateException exception ) {
					throw exception;
				}
				throw new HibernateException( "Unable to check validity of passed ValidatorFactory", e );
			}
			catch (IllegalAccessException e) {
				throw new HibernateException( "Unable to check validity of passed ValidatorFactory", e );
			}
			catch (HibernateException e) {
				throw e;
			}
			catch (Exception e) {
				throw new HibernateException( "Could not locate method needed for ValidatorFactory validation", e );
			}
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Exception e) {
			throw new HibernateException( "Could not locate TypeSafeActivator class", e );
		}
	}

	static void validateMissingBeanValidationApi(
			Set<ValidationMode> modes,
			ValidationConstraintDdlInfluence constraintInfluence) {
		BeanValidationPlan.validateMissingBeanValidationApi( modes, constraintInfluence );
	}

}
