/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class DefaultPackageEntity {
	@Id
	private long id;
}
