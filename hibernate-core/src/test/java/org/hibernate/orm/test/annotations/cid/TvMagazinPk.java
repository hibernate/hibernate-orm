/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;
import java.io.Serializable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class TvMagazinPk implements Serializable {
	@ManyToOne
	@JoinColumn(nullable=false)
	public Channel channel;

	@ManyToOne
	@JoinColumn(nullable=false)
	public Presenter presenter;
}
