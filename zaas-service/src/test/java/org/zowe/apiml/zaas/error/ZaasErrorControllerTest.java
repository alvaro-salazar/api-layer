/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.error;


import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.zaas.error.controllers.ZaasErrorController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZaasErrorControllerTest {

    @Nested
    class GivenNotFoundErrorRequest {

        @BeforeEach
        void init() {
            MessageTemplate messageTemplate = new MessageTemplate("org.zowe.apiml.common.endPointNotFound", "number", MessageType.ERROR, "text");
            Message message = Message.of("org.zowe.apiml.common.endPointNotFound", messageTemplate, new Object[0]);
            MessageService messageService = mock(MessageService.class);

            when(messageService.createMessage(anyString(), (Object[]) any())).thenReturn(message);

        }


    }

    @Test
    void testGenericError() {
        MessageService messageService = new YamlMessageService();
        ZaasErrorController errorController = new ZaasErrorController(messageService);

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, new Exception("Hello"));
        request.setAttribute(ErrorUtils.ATTR_ERROR_STATUS_CODE, 523);
        request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, "/uri");

        ResponseEntity<ApiMessageView> response = errorController.error(request);

        assertEquals("org.zowe.apiml.common.internalRequestError", response.getBody().getMessages().get(0).getMessageKey());
        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains("Hello"));
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains("/uri"));
    }

}
