/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.defaultsemantics.list.sub;

import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import org.hibernate.orm.test.mapping.collections.defaultsemantics.list.Model;

/// @author Steve Ebersole
@Entity
public class ScopeOwner extends Model.Base {
	@ElementCollection public List<String> local;
	@Embedded public Model.Details details;
}
