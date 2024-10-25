/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.zaas;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ZaasExceptionHandlerTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void init() {
        RestAssured.baseURI = "https://localhost";
        RestAssured.port = port;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenUnknownEndpoint_whenCallZaas_thenReturns404WithMessage() {
        given().when()
            .get("/unknown/endpoint")
        .then()
            .statusCode(404)
            .body("messages[0].messageKey", is("org.zowe.apiml.apicatalog.serviceNotFound"));
    }

}
