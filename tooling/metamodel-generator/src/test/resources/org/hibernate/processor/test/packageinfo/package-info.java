@NamedQuery(
        name = "#findByKey",
        query = "from Message where key=:key")
@NamedQuery(
		name = "findByIdAndKey",
		query = "from Message where id=:id and key=:key")

package org.hibernate.processor.test.packageinfo;

import org.hibernate.annotations.NamedQuery;
