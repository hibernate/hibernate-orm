/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.osgitest.command;

import java.util.Map;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.service.command.CommandSession;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.osgitest.DataPointService;

@Command(scope = "dp", name = "getRevisions")
public class GetRevisionsCommand implements Action {
	@Argument(index=0, name="Id", required=true, description="Id", multiValued=false)
    String id;
	
    private DataPointService dpService;
    
    public void setDpService(DataPointService dpService) {
        this.dpService = dpService;
    }

    public Object execute(CommandSession session) throws Exception {
    	Map<Number, DefaultRevisionEntity> revisions = dpService.getRevisions(Long.valueOf( id ));
        for (Number revisionNum : revisions.keySet()) {
        	DefaultRevisionEntity dre = revisions.get( revisionNum );
            System.out.println(revisionNum + ": " + dre.getId() + ", " + dre.getTimestamp());
        }
        return null;
    }

}
