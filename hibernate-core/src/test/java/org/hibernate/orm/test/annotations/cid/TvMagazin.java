/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;
import java.util.Date;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AssociationOverrides({
@AssociationOverride(name = "id.channel", joinColumns = @JoinColumn(name = "chan_id", nullable = false)),
@AssociationOverride(name = "id.presenter", joinColumns = @JoinColumn(name = "presenter_name", nullable = false))})
public class TvMagazin {
	@EmbeddedId
	public TvMagazinPk id;
	@Temporal(TemporalType.TIME)
	@Column(name="`time`")
	Date time;
}
