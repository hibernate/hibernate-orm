/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.securityannotation;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RunAs;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
@DeclareRoles({"admin", "manager", "editor"})
@RolesAllowed("admin")
@RunAs("admin")
public interface SecuredBookRepository {
	@Find
	@DenyAll
	SecuredBook find(String isbn);

	@Find
	@PermitAll
	List<SecuredBook> findAll();

	@Insert
	@RolesAllowed({"manager", "editor"})
	void add(SecuredBook book);
}
