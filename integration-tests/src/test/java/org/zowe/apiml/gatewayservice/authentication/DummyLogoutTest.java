/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice.authentication;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.zowe.apiml.gatewayservice.SecurityUtils;

import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

class DummyLogoutTest extends LogoutTest {
    private static AuthenticationProviders providers = new AuthenticationProviders(SecurityUtils.getGateWayUrl("/authentication"));

    // Change to dummy and run the same test as for the zOSMF
    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        providers.switchProvider("dummy");
    }

    protected String generateToken() {
        return SecurityUtils.gatewayToken("user", "user");
    }

    @AfterAll
    static void switchToDefaultProvider() {
        providers.switchProvider(null);
    }
}
