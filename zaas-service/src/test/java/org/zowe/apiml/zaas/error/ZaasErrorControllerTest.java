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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.apiml.zaas.error.controllers.ZaasErrorController;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;

class ZaasErrorControllerTest {

    private MockMvc mockMvc;

    @Nested
    class GivenNotFoundErrorRequest {

        @BeforeEach
        void init() {
            MessageTemplate messageTemplate = new MessageTemplate("org.zowe.apiml.common.endPointNotFound", "number", MessageType.ERROR, "text");
            Message message = Message.of("org.zowe.apiml.common.endPointNotFound", messageTemplate, new Object[0]);
            MessageService messageService = mock(MessageService.class);

            when(messageService.createMessage(anyString(), (Object[]) any())).thenReturn(message);

            NotFoundErrorController notFoundErrorController = new NotFoundErrorController(messageService);
            MockitoAnnotations.openMocks(this);

            mockMvc = MockMvcBuilders
                .standaloneSetup(notFoundErrorController)
                .build();
        }

        @Test
        void whenCallingWithRequestAttribute_thenReturnProperErrorStatus() throws Exception {
            mockMvc.perform(get("/not_found").requestAttr("javax.servlet.error.status_code", 404))
                .andExpect(status().isNotFound());
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

        assertEquals(523,  response.getStatusCodeValue());
        assertEquals("org.zowe.apiml.common.internalRequestError",  response.getBody().getMessages().get(0).getMessageKey());
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains("Hello"));
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains("/uri"));
    }

}
