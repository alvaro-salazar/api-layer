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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.security.common.auth.saf.EndpointImproprietyConfigureException;
import org.zowe.apiml.security.common.auth.saf.UnsupportedResourceClassException;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.service.saf.SafIdtAuthException;
import org.zowe.apiml.zaas.security.service.saf.SafIdtException;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.zaas.security.ticket.ApplicationNameNotFoundException;

import javax.management.ServiceNotFoundException;
import javax.net.ssl.SSLException;

@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ZaasExceptionHandler {

    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    private static String WWW_AUTHENTICATE_FORMAT = "Basic realm=\"%s\"";
    private static final String DEFAULT_REALM = "Realm";

    private final MessageService messageService;

    @ExceptionHandler(value = {IRRPassTicketGenerationException.class})
    public ResponseEntity<ApiMessageView> handlePassTicketException(IRRPassTicketGenerationException ex) {
        log.error(ex.getMessage());
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed",
            ex.getErrorCode().getMessage()).mapToView();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {SafIdtException.class, SafIdtAuthException.class})
    public ResponseEntity<ApiMessageView> handleSafIdtExceptions(RuntimeException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.idt.failed", ex.getMessage()).mapToView();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {ApplicationNameNotFoundException.class})
    public ResponseEntity<ApiMessageView> handleApplIdNotFoundException(ApplicationNameNotFoundException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.invalidApplicationName").mapToView();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {ServiceNotFoundException.class})
    public ResponseEntity<ApiMessageView> handleServiceNotFoundException(ServiceNotFoundException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.zaas.zosmf.noZosmfTokenReceived", ex.getMessage()).mapToView();
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {IllegalStateException.class})
    public ResponseEntity<ApiMessageView> handleZoweJwtCreationErrors(IllegalStateException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.zaas.zoweJwt.noToken", ex.getMessage()).mapToView();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {TokenNotValidException.class, AuthSchemeException.class})
    public ResponseEntity<ApiMessageView> handleTokenNotValidException(RuntimeException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.zaas.security.invalidToken").mapToView();
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {TokenExpireException.class})
    public ResponseEntity<ApiMessageView> handleTokenExpiredException(TokenExpireException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.zaas.security.expiredToken").mapToView();
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    private static String createHeaderValue(String realm) {
        Assert.notNull(realm, "realm cannot be null");
        return String.format(WWW_AUTHENTICATE_FORMAT, realm);
    }

    public void setWwwAuthenticateResponse(HttpServletResponse response) {
        response.addHeader(WWW_AUTHENTICATE, createHeaderValue(DEFAULT_REALM));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiMessageView> handleAuthenticationException(HttpServletResponse response, AuthenticationException authenticationException) {
        log.debug("Unauthorized access", authenticationException);
        setWwwAuthenticateResponse(response);
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.common.unauthorized").mapToView();
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiMessageView> handleAccessDeniedException(HttpServletRequest request, AccessDeniedException accessDeniedException) {
        log.debug("Unauthenticated access", accessDeniedException);
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.forbidden", request.getRequestURI()).mapToView();
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiMessageView> handleNoResourceFoundException(NoHandlerFoundException e) {
        log.debug("Resource not found", e);
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.common.notFound").mapToView();
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiMessageView> handleMethodNotAllowedException(HttpServletRequest request, HttpRequestMethodNotSupportedException notAllowedMethodException) {
        log.debug("MethodNotAllowedException exception", notAllowedMethodException);
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.invalidMethod", request.getMethod(), request.getRequestURI()).mapToView();
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(SSLException.class)
    public ResponseEntity<ApiMessageView> handleSslException(HttpServletRequest request, SSLException sslException) {
        log.debug("SSL exception", sslException);
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.common.tlsError", request.getRequestURI(), sslException.getMessage()).mapToView();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }


    @ExceptionHandler(UnsupportedResourceClassException.class)
    public ResponseEntity<ApiMessageView> handleUnsupportedResourceClassException(UnsupportedResourceClassException unsupportedResourceClassException) {
        log.debug("Unsupported resource class", unsupportedResourceClassException);
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.common.auth.saf.endpoint.nonZoweClass", unsupportedResourceClassException.getResourceClass()).mapToView();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(EndpointImproprietyConfigureException.class)
    public ResponseEntity<ApiMessageView> handleEndpointImproprietyConfigureException(EndpointImproprietyConfigureException improprietyConfigureException) {
        log.debug("Endpoint is impropriety configured", improprietyConfigureException);
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.common.auth.saf.endpoint.endpointImproprietyConfigure", improprietyConfigureException.getEndpoint()).mapToView();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiMessageView> handleInternalException(Exception exception) {
        log.debug("Unexpected internal error", exception);
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.common.internalServerError").mapToView();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiMessageView> handleUnsupportedMediaException(HttpMediaTypeNotSupportedException exception) {
        log.debug("Requested media type is not supported", exception);
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.common.unsupportedMediaType").mapToView();
        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

}
