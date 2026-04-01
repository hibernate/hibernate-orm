/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.support;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.QueueType;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Map;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;

/**
 * @author Steve Ebersole
 */
public class ActionQueueFactoryServiceInitiator implements StandardServiceInitiator<ActionQueueFactoryService> {
	public static final ActionQueueFactoryServiceInitiator INSTANCE = new ActionQueueFactoryServiceInitiator();

	@Override
	public @Nullable ActionQueueFactoryService initiateService(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry) {
		var setting = ConfigurationHelper.getString( FLUSH_QUEUE_TYPE, configurationValues, "legacy" );
		return new ActionQueueFactoryService( QueueType.fromSetting( setting ) );
	}

	@Override
	public Class<ActionQueueFactoryService> getServiceInitiated() {
		return ActionQueueFactoryService.class;
	}
}
