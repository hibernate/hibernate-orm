/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

import java.util.Properties;

import org.hibernate.boot.BootLogging;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.ParameterizedType;

/**
 * ManagedBean implementation for delayed {@link ParameterizedType}
 * handling (parameter injection) for a UserCollectionType
 *
 * @author Steve Ebersole
 */
public class DelayedParameterizedTypeBean<T> implements ManagedBean<T> {
	private final ManagedBean<T> underlyingBean;
	private final Properties properties;

	private T instance;

	public DelayedParameterizedTypeBean(ManagedBean<T> underlyingBean, Properties properties) {
		assert ParameterizedType.class.isAssignableFrom( underlyingBean.getBeanClass() );
		this.underlyingBean = underlyingBean;
		this.properties = properties;
	}

	@Override
	public Class<T> getBeanClass() {
		return underlyingBean.getBeanClass();
	}

	@Override
	public T getBeanInstance() {
		if ( instance == null ) {
			instance = underlyingBean.getBeanInstance();
			( (ParameterizedType) instance ).setParameterValues( properties );
		}
		return instance;
	}

	/**
	 * Create a bean wrapper which delays parameter injection
	 * until the bean instance is needed if there are parameters
	 */
	public static <T> ManagedBean<T> delayedConfigBean(
			String role,
			ManagedBean<T> bean,
			Properties properties) {
		if ( CollectionHelper.isNotEmpty( properties ) ) {
			if ( ParameterizedType.class.isAssignableFrom( bean.getBeanClass() ) ) {
				return new DelayedParameterizedTypeBean<>( bean, properties );
			}

			// there were parameters, but the custom-type does not implement the interface
			// used to inject them - log a "warning"
			BootLogging.BOOT_LOGGER.debugf(
					"`@CollectionType` (%s) specified parameters, but the" +
							" implementation does not implement `%s` which is used to inject them - `%s`",
					role,
					ParameterizedType.class.getName(),
					bean.getBeanClass().getName()
			);
		}

		return bean;
	}
}
