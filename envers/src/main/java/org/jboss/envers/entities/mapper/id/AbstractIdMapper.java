/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.id;

import org.jboss.envers.tools.query.Parameters;

import java.util.Iterator;
import java.util.List;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractIdMapper implements IdMapper {
    private Parameters getParametersToUse(Parameters parameters, List<QueryParameterData> paramDatas) {
         if (paramDatas.size() > 1) {
            return parameters.addSubParameters("and");
        } else {
            return parameters;
        }
    }

    public void addIdsEqualToQuery(Parameters parameters, String prefix1, String prefix2) {
        List<QueryParameterData> paramDatas = mapToQueryParametersFromId(null);

        Parameters parametersToUse = getParametersToUse(parameters, paramDatas);

        for (QueryParameterData paramData : paramDatas) {
            parametersToUse.addWhere(paramData.getProperty(prefix1), false, "=", paramData.getProperty(prefix2), false);
        }
    }

    public void addIdsEqualToQuery(Parameters parameters, String prefix1, IdMapper mapper2, String prefix2) {
        List<QueryParameterData> paramDatas1 = mapToQueryParametersFromId(null);
        List<QueryParameterData> paramDatas2 = mapper2.mapToQueryParametersFromId(null);

        Parameters parametersToUse = getParametersToUse(parameters, paramDatas1);

        Iterator<QueryParameterData> paramDataIter1 = paramDatas1.iterator();
        Iterator<QueryParameterData> paramDataIter2 = paramDatas2.iterator();
        while (paramDataIter1.hasNext()) {
            QueryParameterData paramData1 = paramDataIter1.next();
            QueryParameterData paramData2 = paramDataIter2.next();

            parametersToUse.addWhere(paramData1.getProperty(prefix1), false, "=", paramData2.getProperty(prefix2), false);
        }
    }

    public void addIdEqualsToQuery(Parameters parameters, Object id, String prefix, boolean equals) {
        List<QueryParameterData> paramDatas = mapToQueryParametersFromId(id);

        Parameters parametersToUse = getParametersToUse(parameters, paramDatas);

        for (QueryParameterData paramData : paramDatas) {
            parametersToUse.addWhereWithParam(paramData.getProperty(prefix), equals ? "=" : "<>", paramData.getValue());
        }
    }

    public void addNamedIdEqualsToQuery(Parameters parameters, String prefix, boolean equals) {
        List<QueryParameterData> paramDatas = mapToQueryParametersFromId(null);

        Parameters parametersToUse = getParametersToUse(parameters, paramDatas);

        for (QueryParameterData paramData : paramDatas) {
            parametersToUse.addWhereWithNamedParam(paramData.getProperty(prefix), equals ? "=" : "<>", paramData.getQueryParameterName());
        }
    }
}
