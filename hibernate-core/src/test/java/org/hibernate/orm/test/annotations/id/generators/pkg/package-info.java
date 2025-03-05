/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
@SequenceGenerator(initialValue = 42)
@TableGenerator(initialValue = 69)
package org.hibernate.orm.test.annotations.id.generators.pkg;

import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
