/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.helpdesk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "user_accounts" )
public class Account {
	private Integer id;

	private Status loginStatus;
	private Status systemAccessStatus;
	private Status serviceStatus;

	public Account() {
	}

	public Account(
			Integer id,
			Status loginStatus,
			Status systemAccessStatus,
			Status serviceStatus) {
		this.id = id;
		this.loginStatus = loginStatus;
		this.systemAccessStatus = systemAccessStatus;
		this.serviceStatus = serviceStatus;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Enumerated( EnumType.ORDINAL )
	public Status getLoginStatus() {
		return loginStatus;
	}

	public void setLoginStatus(Status loginStatus) {
		this.loginStatus = loginStatus;
	}

	@Enumerated( EnumType.STRING )
	public Status getSystemAccessStatus() {
		return systemAccessStatus;
	}

	public void setSystemAccessStatus(Status systemAccessStatus) {
		this.systemAccessStatus = systemAccessStatus;
	}

	@Convert( converter = ServiceStatusConverter.class )
	public Status getServiceStatus() {
		return serviceStatus;
	}

	public void setServiceStatus(Status serviceStatus) {
		this.serviceStatus = serviceStatus;
	}

	@Converter( autoApply = false )
	private static class ServiceStatusConverter implements AttributeConverter<Status,Integer> {

		@Override
		public Integer convertToDatabaseColumn(Status attribute) {
			if ( attribute == null ) {
				return null;
			}

			return attribute.getCode();
		}

		@Override
		public Status convertToEntityAttribute(Integer dbData) {
			if ( dbData == null ) {
				return null;
			}

			return Status.fromCode( dbData );
		}
	}
}
