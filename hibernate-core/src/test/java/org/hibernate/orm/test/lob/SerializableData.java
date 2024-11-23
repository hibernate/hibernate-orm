/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;
import java.io.Serializable;

/**
 * Implementation of SerializableData.
 *
 * @author Steve
 */
public class SerializableData implements Serializable
{
private String payload;

public SerializableData(String payload)
{
	this.payload = payload;
}

public String getPayload()
{
	return payload;
}

public void setPayload(String payload)
{
	this.payload = payload;
}
}
