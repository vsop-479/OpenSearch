/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.wildfly.transport;

import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.wildfly.model.Employee;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

@Path("/employees")
public class RestHighLevelClientEmployeeResource {

    @Inject
    private RestHighLevelClient client;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmployeeById(final @PathParam("id") Long id) throws IOException {
        Objects.requireNonNull(id);
        final GetResponse response = client.get(new GetRequest("megacorp", Long.toString(id)), RequestOptions.DEFAULT);
        if (response.isExists()) {
            final Map<String, Object> source = response.getSource();
            final Employee employee = new Employee();
            employee.setFirstName((String) source.get("first_name"));
            employee.setLastName((String) source.get("last_name"));
            employee.setAge((Integer) source.get("age"));
            employee.setAbout((String) source.get("about"));
            @SuppressWarnings("unchecked")
            final List<String> interests = (List<String>) source.get("interests");
            employee.setInterests(interests);
            return Response.ok(employee).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putEmployeeById(final @PathParam("id") Long id, final Employee employee) throws URISyntaxException, IOException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(employee);
        try (XContentBuilder builder = jsonBuilder()) {
            builder.startObject();
            {
                builder.field("first_name", employee.getFirstName());
                builder.field("last_name", employee.getLastName());
                builder.field("age", employee.getAge());
                builder.field("about", employee.getAbout());
                if (employee.getInterests() != null) {
                    builder.startArray("interests");
                    {
                        for (final String interest : employee.getInterests()) {
                            builder.value(interest);
                        }
                    }
                    builder.endArray();
                }
            }
            builder.endObject();
            final IndexRequest request = new IndexRequest("megacorp");
            request.id(Long.toString(id));
            request.source(builder);
            final IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            if (response.status().getStatus() == 201) {
                return Response.created(new URI("/employees/" + id)).build();
            } else {
                return Response.ok().build();
            }
        }
    }

}
