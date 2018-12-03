/**
 * Copyright 2014 Flipkart Internet Pvt. Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flipkart.foxtrot.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.flipkart.foxtrot.core.exception.FoxtrotException;
import com.flipkart.foxtrot.server.console.ConsolePersistence;
import com.flipkart.foxtrot.server.console.ConsoleV2;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/v2/consoles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConsoleV2Resource {

    private ConsolePersistence consolePersistence;

    public ConsoleV2Resource(ConsolePersistence consolePersistence) {
        this.consolePersistence = consolePersistence;
    }

    @POST
    @Timed
    public ConsoleV2 save(ConsoleV2 console) throws FoxtrotException {
        consolePersistence.saveV2(console);
        return console;
    }

    @GET
    @Timed
    @Path("/{id}")
    public ConsoleV2 get(@PathParam("id") final String id) throws FoxtrotException {
        return consolePersistence.getV2(id);
    }

    @DELETE
    @Path("/{id}/delete")
    @Timed
    public void delete(@PathParam("id") final String id) throws FoxtrotException {
        consolePersistence.deleteV2(id);
    }

    @GET
    @Timed
    public List<ConsoleV2> getList() throws FoxtrotException {
        return consolePersistence.getV2();
    }

    @GET
    @Timed
    @Path("/{id}/old/get")
    public ConsoleV2 getOldVersion(@PathParam("id") final String id) throws FoxtrotException {
        return consolePersistence.getOldVersion(id);
    }

    @GET
    @Timed
    @Path("/{name}/old")
    public List<ConsoleV2> getOldVersionList(@PathParam("name") final String name) throws FoxtrotException {
        return consolePersistence.getAllOldVersions(name);
    }

    @DELETE
    @Path("/{id}/old/delete")
    @Timed
    public void deleteOldVersion(@PathParam("id") final String id) throws FoxtrotException {
        consolePersistence.deleteOldVersion(id);
    }

    @GET
    @Timed
    @Path("/{id}/old/set/current")
    public void setOldVersionAsCurrent(@PathParam("id") final String id) throws FoxtrotException {
        consolePersistence.setOldVersionAsCurrent(id);
    }
}
